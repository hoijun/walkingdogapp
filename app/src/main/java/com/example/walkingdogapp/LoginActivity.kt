package com.example.walkingdogapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.databinding.ActivityLoginBinding
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.system.exitProcess

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginInfo: android.content.SharedPreferences
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.database
    private var backPressedTime : Long = 0
    private val loadingDialogFragment = LoadingDialogFragment()

    private val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e(TAG, "로그인 실패 $error")
            setLoginIngView(false)
        } else if (token != null) {
            Log.e(TAG, "로그인 성공 ${token.accessToken}")
            loginApp("kakao")
        }
    }
    private val naverLoginCallback = object : OAuthLoginCallback {
        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
            val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
            setLoginIngView(false)
            Log.e("test", "$errorCode $errorDescription")
        }

        override fun onSuccess() {
            loginApp("naver")
        }
    }
    private val naverProfileCallback = object : NidProfileCallback<NidProfileResponse> {
        override fun onSuccess(result: NidProfileResponse) {
            if(result.profile?.email == null || result.profile?.id == null) {
                setLoginIngView(false)
                return
            }
            saveUser(result.profile!!.email!!, result.profile!!.id!!)
            signupFirebase(result.profile!!.email!!, result.profile!!.id!!)
        }

        override fun onError(errorCode: Int, message: String) {
            setLoginIngView(false)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            setLoginIngView(false)
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backPressedTime < 2500) {
                moveTaskToBack(true)
                finishAndRemoveTask()
                exitProcess(0)
            }
            Toast.makeText(this@LoginActivity, "한번 더 클릭 시 종료 됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)

        auth = FirebaseAuth.getInstance()
        loginInfo = getSharedPreferences("setting", MODE_PRIVATE)

        binding.apply {
            KakaoLogin.setOnClickListener {
                try {
                    loginKakao()
                    setLoginIngView(true)
                } catch (e: Exception) {
                    e.message?.let { it1 -> Log.d("error", it1) }
                    setLoginIngView(false)
                }
            }
            NaverLogin.setOnClickListener {
                NaverIdLoginSDK.authenticate(this@LoginActivity, naverLoginCallback)
                setLoginIngView(true)
            }
        }
    }

    private fun loginKakao() {
        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoAccount(
                this,
                callback = kakaoLoginCallback
            ) // 카카오 이메일 로그인
            return
        }

        UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
            if (error != null) {
                Log.e(TAG, "로그인 실패", error)
                setLoginIngView(false)
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    return@loginWithKakaoTalk
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(
                        this,
                        callback = kakaoLoginCallback
                    )
                }
            } else if (token != null) {
                Log.e(TAG, "로그인 성공 ${token.accessToken}")
                loginApp("kakao")
            }
        }
    }


    private fun loginApp(sns: String) {
        if (sns == "kakao") {
            UserApiClient.instance.me { user, error1 ->
                if (error1 != null) {
                    Log.e(TAG, "사용자 정보 요청 실패", error1)
                    setLoginIngView(false)
                } else if (user?.kakaoAccount?.email != null) {
                    saveUser(user.kakaoAccount!!.email!!, user.id.toString())
                    signupFirebase(user.kakaoAccount!!.email!!, user.id.toString())
                } else if (user?.kakaoAccount?.email == null) {
                    setLoginIngView(false)
                }
            }
        } else if (sns == "naver") {
            NidOAuthLogin().callProfileApi(naverProfileCallback)
        }
    }

    private fun signupFirebase(myEmail: String, password: String) {
        auth.createUserWithEmailAndPassword(myEmail, password).addOnCompleteListener {
            if (!it.isSuccessful) {
                if (it.exception is FirebaseAuthUserCollisionException) {
                    //이미 가입된 이메일일 경우
                    signinFirebase(myEmail, password)
                } else {
                    //예외메세지가 있다면 출력
                    //에러가 났다거나 서버가 연결이 실패했다거나
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_LONG).show()
                    setLoginIngView(false)
                }
                return@addOnCompleteListener
            }

            val uid = auth.currentUser?.uid

            val userRef = db.getReference("Users")
            val userInfo = UserInfo(email = myEmail)

            lifecycleScope.launch {
                val userInfoJob = async(Dispatchers.IO) {
                    try {
                        userRef.child("$uid").child("user").setValue(userInfo).await()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                        setLoginIngView(false)
                        return@async
                    }
                }

                val totalWalkInfoJob = async(Dispatchers.IO) {
                    try {
                        userRef.child("$uid").child("totalWalk").setValue(WalkInfo()).await()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                        setLoginIngView(false)
                        return@async
                    }
                }

                val collectionInfoJob = async(Dispatchers.IO) {
                    try {
                        userRef.child("$uid").child("collection").setValue(Constant.item_whether)
                            .await()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                        setLoginIngView(false)
                        return@async
                    }
                }

                userInfoJob.await()
                totalWalkInfoJob.await()
                collectionInfoJob.await()

                startMain()
            }
        }
    }

    private fun signinFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {  //통신 완료가 된 후 무슨일을 할지
                task ->
            if (task.isSuccessful) {
                //로그인 처리를 해주면 됨!
                startMain()
            } else {
                // 오류가 난 경우!
                Toast.makeText(this, task.exception?.message + " 2", Toast.LENGTH_LONG).show()
                setLoginIngView(false)
            }
        }
    }

    private fun saveUser(email: String, password: String) {
        val editor = loginInfo.edit()
        editor.putString("id",email)
        editor.putString("password",password)
        editor.apply()
    }

    private fun startMain() {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(mainIntent)
    }

    private fun setLoginIngView(loginIng: Boolean) {
        if (loginIng) {
            loadingDialogFragment.show(this.supportFragmentManager, "loading")
        } else {
            loadingDialogFragment.dismiss()
        }
    }
}