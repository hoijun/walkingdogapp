package com.tulmunchi.walkingdogapp.presentation.ui.mypage.settingPage

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.tulmunchi.walkingdogapp.core.analytics.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.core.datastore.UserPreferencesDataStore
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentSettingBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.core.dialog.WriteDialog
import com.tulmunchi.walkingdogapp.presentation.ui.alarm.AlarmFunctions
import com.tulmunchi.walkingdogapp.presentation.ui.login.LoginActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private var mainActivity: MainActivity? = null
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val auth = FirebaseAuth.getInstance()
    private var email = ""
    private var password = ""

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
        }
    }

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStore

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    private var loadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        loadingDialog = loadingDialogFactory.create(parentFragmentManager)
        binding.apply {
            btnGoMypage.setOnClickListener {
                goMyPage()
            }

            // 버전 정보 설정
            tulmunchiVersion = "버전정보 v${context?.packageManager?.getPackageInfo(
                context?.packageName ?: "",
                0
            )?.versionName ?: "Unknown"}"

            lifecycleScope.launch {
                email = userPreferencesDataStore.getEmail().first() ?: ""
                password = userPreferencesDataStore.getPassword().first() ?: ""
                userEmail = email
            }

            logoutBtn.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }

                if (email.contains("@naver.com")) { // 네이버로 로그인 했을 경우
                    try {
                        NaverIdLoginSDK.logout()
                        auth.signOut()
                        successLogout()
                    } catch (e: Exception) {
                        failLogout(e.message.toString(), "Naver")
                    }
                } else { // 카카오로 로그인 했을 경우
                    UserApiClient.instance.logout { error ->
                        if (error != null) {
                            failLogout(error.message.toString(), "Kakao")
                        } else {
                            auth.signOut()
                            successLogout()
                        }
                    }
                }
            }

            settingInquiry.setOnClickListener {
                activity?.let {
                    sendEmail(it)
                }
            }

            settingPrivacyPolicy.setOnClickListener {
                goWebView("https://hoitho.tistory.com/1")
            }

            settingTermsofservice.setOnClickListener {
                goWebView("https://hoitho.tistory.com/2")
            }

            settingTermofLocation.setOnClickListener {
                goWebView("https://hoitho.tistory.com/3")
            }

            settingCopyright.setOnClickListener {
                goWebView("https://hoitho.tistory.com/4")
            }

            settingWithdrawal.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }

                parentFragmentManager.let {
                    try {
                        showLoadingFragment()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val credential = EmailAuthProvider.getCredential(email, password)
                auth.currentUser?.reauthenticate(credential)?.addOnSuccessListener {
                    hideLoadingDialog()
                    val writeDialog = WriteDialog()
                    writeDialog.clickYesListener = WriteDialog.OnClickYesListener { writeText ->
                        if (userEmail != writeText) {
                            toastMsg("이메일이 일치하지 않습니다.")
                            return@OnClickYesListener
                        } // 이메일 올바르게 입력x

                        parentFragmentManager.let {
                            try {
                                hideLoadingDialog()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        lifecycleScope.launch {
                            if (email.contains("@naver.com")) { // 네이버로 로그인 했을 경우
                                if (!mainViewModel.deleteAccount()) {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    hideLoadingDialog()
                                    return@launch
                                }

                                auth.currentUser?.delete()?.addOnSuccessListener {
                                    NidOAuthLogin().callDeleteTokenApi(object :
                                        OAuthLoginCallback {
                                        override fun onError(errorCode: Int, message: String) {
                                            onFailure(errorCode, message)
                                        }

                                        override fun onFailure(
                                            httpStatus: Int,
                                            message: String,
                                        ) {
                                            completeDeleteAccount()
                                            hideLoadingDialog()
                                        }

                                        override fun onSuccess() {
                                            completeDeleteAccount()
                                            hideLoadingDialog()
                                        }
                                    })
                                }?.addOnFailureListener {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    hideLoadingDialog()
                                }
                            } else { // 카카오로 로그인 했을 경우
                                if (!mainViewModel.deleteAccount()) {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    hideLoadingDialog()
                                    return@launch
                                }

                                auth.currentUser?.delete()?.addOnSuccessListener {
                                    UserApiClient.instance.unlink {
                                        completeDeleteAccount()
                                        hideLoadingDialog()
                                    }
                                }?.addOnFailureListener {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    hideLoadingDialog()
                                }
                            }
                        }
                    }
                    val bundle = Bundle()
                    bundle.putString("text", "이메일을 입력해주세요.")
                    writeDialog.arguments = bundle

                    parentFragmentManager.let {
                        try {
                            writeDialog.show(it, "writeEmail")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }?.addOnFailureListener {
                    toastMsg("앱을 껏다 켜주세요!")
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun goMyPage() {
        mainActivity?.changeFragment(MyPageFragment())
    }

    private fun goLogin() {
        context?.let { ctx ->
            val loginIntent = Intent(ctx, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
        }
    }

    private fun completeDeleteAccount() {
        lifecycleScope.launch {
            userPreferencesDataStore.clearAll()
        }
        removeAlarms()
        goLogin()
    }

    private fun successLogout() {
        lifecycleScope.launch {
            userPreferencesDataStore.clearAll()
        }
        goLogin()
        removeAlarms()
        toastMsg("로그아웃 성공")
    }

    private fun failLogout(msg: String, api: String) {
        toastMsg("로그아웃 실패")
        firebaseHelper.logEvent(
            listOf(
                "type" to "Logout_Fail",
                "api" to api,
                "msg" to msg
            )
        )
    }

    private fun removeAlarms() {
        context?.let { ctx ->
            val alarmFunctions = AlarmFunctions(ctx)
            coroutineScope.launch {
                val alarms = mainViewModel.alarms.value ?: emptyList()
                for (alarm in alarms) {
                    alarmFunctions.cancelAlarm(alarm.alarmCode)
                    mainViewModel.deleteAlarm(alarm.alarmCode)
                }
            }
        }
    }

    private fun toastMsg(msg: String) {
        context?.let { ctx ->
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun goWebView(uri: String) {
        context?.let { ctx ->
            val intent = Intent(ctx, PrivacyWebViewActivity::class.java).apply {
                putExtra("uri", uri)
            }
            startActivity(intent)
        }
    }

    private fun sendEmail(context: Context) {
        val emailSelectorIntent = Intent(Intent.ACTION_SENDTO).apply {
            setData("mailto:".toUri())
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf("ghlwns10@naver.com"))
            putExtra(Intent.EXTRA_SUBJECT, "문의 제목")
            putExtra(Intent.EXTRA_TEXT, "문의 내용을 여기에 작성하세요.")

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            setSelector(emailSelectorIntent)
        }

        try {
            context.startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "이메일 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingFragment() {
        if (isDetached) {
            return
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        if (isDetached) {
            return
        }
        loadingDialog?.dismiss()
    }
}