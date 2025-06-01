package com.tulmunchi.walkingdogapp.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tulmunchi.walkingdogapp.LoadingDialogFragment
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.databinding.ActivityLoginBinding
import com.tulmunchi.walkingdogapp.utils.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.viewmodel.LoginViewModel
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.tulmunchi.walkingdogapp.utils.utils.Utils.Companion.LOADING_DIALOG_TAG

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.database
    private val loginViewModel: LoginViewModel by viewModels()
    private var backPressedTime: Long = 0
    private var shouldShowPermissionDialog = false

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    private val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            setLoginIngView(false)
            toastFailSignUp("Kakao", error.message.toString())
        } else if (token != null) {
            loginApp("kakao")
        }
    }

    private val naverLoginCallback = object : OAuthLoginCallback {
        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            setLoginIngView(false)
            toastFailSignUp("Naver", message)
        }

        override fun onSuccess() {
            loginApp("naver")
        }
    }

    private val naverProfileCallback = object : NidProfileCallback<NidProfileResponse> {
        override fun onSuccess(result: NidProfileResponse) {
            if (result.profile?.email == null || result.profile?.id == null) {
                setLoginIngView(false)
                return
            }
            signupFirebase(result.profile!!.email!!, result.profile!!.id!!)
        }

        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            toastFailSignUp("Naver", message)
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

        binding.apply {
            KakaoLoginBtn.setOnClickListener {
                if (!NetworkManager.checkNetworkState(this@LoginActivity)) {
                    return@setOnClickListener
                }
                setLoginIngView(true)
                loginKakao()
            }

            NaverLoginBtn.setOnClickListener {
                if (!NetworkManager.checkNetworkState(this@LoginActivity)) {
                    return@setOnClickListener
                }
                setLoginIngView(true)
                NaverIdLoginSDK.authenticate(this@LoginActivity, naverLoginCallback)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }

    override fun onResume() {
        super.onResume()
        if (shouldShowPermissionDialog) {
            shouldShowPermissionDialog = false
            startMain()
        }
    }

    private fun loginKakao() {
        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoAccount(
                this,
                callback = kakaoLoginCallback
            )
            return
        }

        UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
            if (error != null) {
                setLoginIngView(false)
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    toastFailSignUp("Kakao", error.message.toString())
                    return@loginWithKakaoTalk
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(
                        this,
                        callback = kakaoLoginCallback
                    )
                }
            } else if (token != null) {
                loginApp("kakao")
            }
        }
    }


    private fun loginApp(sns: String) {
        if (sns == "kakao") {
            UserApiClient.instance.me { user, error1 ->
                if (error1 != null) {
                    toastFailSignUp("Kakao", error1.message.toString())
                    setLoginIngView(false)
                } else if (user?.kakaoAccount?.email != null) {
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
                    saveEmail(this, myEmail, password)
                    signInFirebase(myEmail, password)
                } else {
                    toastFailSignUp("Firebase", it.exception?.message.toString())
                    setLoginIngView(false)
                }
                return@addOnCompleteListener
            }

            auth.currentUser?.delete()?.addOnCompleteListener {
                setLoginIngView(false)
                val termsOfServiceDialog = TermOfServiceDialog()
                termsOfServiceDialog.onClickYesListener = TermOfServiceDialog.OnClickYesListener { agree ->
                    setLoginIngView(true)
                    auth.createUserWithEmailAndPassword(myEmail, password).addOnCompleteListener {
                        if (agree) {
                            loginViewModel.signUp(myEmail)
                            loginViewModel.successSignUp.observe(this@LoginActivity) { success ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    if (success) {
                                        setLoginIngView(false)
                                        saveEmail(this@LoginActivity, myEmail, password)
                                        startMain()
                                        return@launch
                                    }

                                    try {
                                        val uid = auth.currentUser?.uid
                                        val userRef = db.getReference("Users")
                                        userRef.child("$uid").removeValue().await()
                                    } catch (_: Exception) { }
                                    auth.currentUser?.delete()
                                    withContext(Dispatchers.Main) {
                                        toastFailSignUp("Firebase", "")
                                        setLoginIngView(false)
                                        return@withContext
                                    }
                                    return@launch
                                }
                            }
                        }
                    }.addOnFailureListener { error ->
                        toastFailSignUp("Firebase", error.toString())
                        setLoginIngView(false)
                    }
                }
                termsOfServiceDialog.show(supportFragmentManager, "terms")
            }?.addOnFailureListener { error ->
                toastFailSignUp("Firebase", error.toString())
            }
        }
    }

    private fun signInFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener

            if (task.isSuccessful) {
                loginViewModel.setUser()
                startMain()
            } else {
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                setLoginIngView(false)
            }
        }
    }

    private fun startMain() {
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) {
            shouldShowPermissionDialog = true
            return
        }

        val permissionGuideDialog = PermissionGuideDialog()

        permissionGuideDialog.onClickYesListener = PermissionGuideDialog.OnClickYesListener {
            goMain()
        }

        try {
            permissionGuideDialog.show(supportFragmentManager, "permission")
        } catch (e: IllegalStateException) {
            // Dialog 실패 시 바로 메인으로 이동
            goMain()
        }
    }

    private fun goMain() {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(mainIntent)
    }

    private fun setLoginIngView(loginIng: Boolean) {
        if (isFinishing || isDestroyed) {
            return
        }

        if (loginIng) {
            showLoadingFragment()
        } else {
            hideLoadingDialog()
        }
    }

    private fun showLoadingFragment() {
        val existingDialog = supportFragmentManager.findFragmentByTag(LOADING_DIALOG_TAG)
        if (existingDialog != null && !existingDialog.isDetached) {
            return
        }

        try {
            if (!supportFragmentManager.isStateSaved) {
                val loadingDialog = LoadingDialogFragment()
                loadingDialog.show(supportFragmentManager, LOADING_DIALOG_TAG)
            }
        } catch (_: IllegalStateException) { }
    }

    private fun hideLoadingDialog() {
        val loadingDialog = supportFragmentManager.findFragmentByTag(LOADING_DIALOG_TAG) as? DialogFragment
        loadingDialog?.let {
            try {
                if (it.isAdded && !it.isDetached) {
                    it.dismissAllowingStateLoss()
                }
            } catch (_: IllegalStateException) { }
        }
    }

    private fun toastFailSignUp(api: String, reason: String) {
        firebaseHelper.logEvent(
            listOf(
                "type" to "Login_Fail",
                "api" to api,
                "reason" to reason
            )
        )

        runOnUiThread {
            Toast.makeText(
                this@LoginActivity,
                "회원가입 실패",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveEmail(context: Context, email: String, password: String) {
        val sharedPreferences = context.getSharedPreferences("UserEmail", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString("email", email)
            putString("password", password)
        }
    }
}