package com.tulmunchi.walkingdogapp.presentation.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.core.analytics.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.core.datastore.UserPreferencesDataStore
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.databinding.ActivityLoginBinding
import com.tulmunchi.walkingdogapp.presentation.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.database
    private val loginViewModel: LoginViewModel by viewModels()
    private var backPressedTime: Long = 0
    private var shouldShowPermissionDialog = false

    companion object {
        private const val SNS_KAKAO = "kakao"
        private const val SNS_NAVER = "naver"
        private const val BACK_PRESS_INTERVAL = 2500L
    }

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private var loadingDialog: LoadingDialog? = null

    private val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            setLoadingState(false)
            toastFailSignUp("Kakao", error.message.toString())
        } else if (token != null) {
            loginApp(SNS_KAKAO)
        }
    }

    private val naverLoginCallback = object : OAuthLoginCallback {
        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            setLoadingState(false)
            toastFailSignUp("Naver", message)
        }

        override fun onSuccess() {
            loginApp(SNS_NAVER)
        }
    }

    private val naverProfileCallback = object : NidProfileCallback<NidProfileResponse> {
        override fun onSuccess(result: NidProfileResponse) {
            if (result.profile?.email == null || result.profile?.id == null) {
                setLoadingState(false)
                return
            }
            signupFirebase(result.profile!!.email!!, result.profile!!.id!!)
        }

        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }

        override fun onFailure(httpStatus: Int, message: String) {
            toastFailSignUp("Naver", message)
            setLoadingState(false)
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backPressedTime < BACK_PRESS_INTERVAL) {
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

        loadingDialog = loadingDialogFactory.create(supportFragmentManager)

        setupViewModelObservers()

        binding.apply {
            KakaoLoginBtn.setOnClickListener {
                if (!networkChecker.isNetworkAvailable()) {
                    Toast.makeText(this@LoginActivity, "네트워크 연결을 확인해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                setLoadingState(true)
                loginKakao()
            }

            NaverLoginBtn.setOnClickListener {
                if (!networkChecker.isNetworkAvailable()) {
                    Toast.makeText(this@LoginActivity, "네트워크 연결을 확인해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                setLoadingState(true)
                NaverIdLoginSDK.authenticate(this@LoginActivity, naverLoginCallback)
            }
        }
    }

    private fun setupViewModelObservers() {
        // 로딩 상태 관찰
        loginViewModel.isLoading.observe(this) { isLoading ->
            setLoadingState(isLoading)
        }

        // 에러 관찰
        loginViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 성공 관찰
        loginViewModel.signUpSuccess.observe(this) { success ->
            lifecycleScope.launch(Dispatchers.Main) {
                if (success) {
                    setLoadingState(false)
                    auth.currentUser?.email?.let { email ->
                        saveEmail(email, "")
                    }
                    startMain()
                } else {
                    try {
                        val uid = auth.currentUser?.uid
                        val userRef = db.getReference("Users")
                        userRef.child("$uid").removeValue().await()
                    } catch (_: Exception) { }
                    auth.currentUser?.delete()
                    withContext(Dispatchers.Main) {
                        toastFailSignUp("Firebase", "")
                        setLoadingState(false)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        setLoadingState(false)
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
                setLoadingState(false)
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
                loginApp(SNS_KAKAO)
            }
        }
    }


    private fun loginApp(sns: String) {
        when (sns) {
            SNS_KAKAO -> {
                UserApiClient.instance.me { user, error1 ->
                    if (error1 != null) {
                        toastFailSignUp("Kakao", error1.message.toString())
                        setLoadingState(false)
                    } else if (user?.kakaoAccount?.email != null) {
                        signupFirebase(user.kakaoAccount!!.email!!, user.id.toString())
                    } else {
                        setLoadingState(false)
                    }
                }
            }
            SNS_NAVER -> {
                NidOAuthLogin().callProfileApi(naverProfileCallback)
            }
        }
    }

    private fun signupFirebase(myEmail: String, password: String) {
        auth.createUserWithEmailAndPassword(myEmail, password).addOnCompleteListener {
            if (!it.isSuccessful) {
                if (it.exception is FirebaseAuthUserCollisionException) {
                    //이미 가입된 이메일일 경우
                    saveEmail(myEmail, password)
                    signInFirebase(myEmail, password)
                } else {
                    toastFailSignUp("Firebase", it.exception?.message.toString())
                    setLoadingState(false)
                }
                return@addOnCompleteListener
            }

            auth.currentUser?.delete()?.addOnCompleteListener {
                setLoadingState(false)
                val termsOfServiceDialog = TermOfServiceDialog()
                termsOfServiceDialog.onClickYesListener = TermOfServiceDialog.OnClickYesListener { agree ->
                    setLoadingState(true)
                    auth.createUserWithEmailAndPassword(myEmail, password).addOnCompleteListener {
                        if (agree) {
                            saveEmail(myEmail, password)
                            loginViewModel.signUp(myEmail)
                        }
                    }.addOnFailureListener { error ->
                        toastFailSignUp("Firebase", error.toString())
                        setLoadingState(false)
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
                startMain()
            } else {
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                setLoadingState(false)
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

    private fun setLoadingState(isLoading: Boolean) {
        if (isFinishing || isDestroyed) {
            return
        }

        if (isLoading) {
            loadingDialog?.show()
        } else {
            loadingDialog?.dismiss()
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

    private fun saveEmail(email: String, password: String) {
        lifecycleScope.launch {
            userPreferencesDataStore.saveCredentials(email, password)
        }
    }
}