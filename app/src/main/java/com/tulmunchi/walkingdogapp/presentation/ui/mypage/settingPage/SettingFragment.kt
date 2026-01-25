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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.tulmunchi.walkingdogapp.core.analytics.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentSettingBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.core.dialog.WriteDialog
import com.tulmunchi.walkingdogapp.presentation.ui.alarm.AlarmFunctions
import com.tulmunchi.walkingdogapp.presentation.ui.login.LoginActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.SettingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateToMyPage()
        }
    }

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    @Inject
    lateinit var navigationManager: NavigationManager

    private var loadingDialog: LoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        loadingDialog = loadingDialogFactory.create(parentFragmentManager)

        setupObservers()
        setupViews()

        return binding.root
    }

    private fun setupObservers() {
        settingViewModel.email.observe(viewLifecycleOwner) { email ->
            binding.userEmail = email
        }

        settingViewModel.logoutSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                settingViewModel.clearLogoutSuccess()
                navigateToLogin()
                removeAlarms()
            }
        }

        settingViewModel.deleteAccountSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                settingViewModel.clearDeleteAccountSuccess()
                removeAlarms()
                navigateToLogin()
            }
        }
    }

    private fun setupViews() {
        binding.apply {
            btnNavigateToMyPage.setOnClickListener {
                navigateToMyPage()
            }

            // 버전 정보 설정
            tulmunchiVersion = "버전정보 v${context?.packageManager?.getPackageInfo(
                context?.packageName ?: "",
                0
            )?.versionName ?: "Unknown"}"

            settingLogout.setOnClickListener {
                handleLogout()
            }

            settingInquiry.setOnClickListener {
                activity?.let {
                    sendEmail(it)
                }
            }

            settingPrivacyPolicy.setOnClickListener {
                navigateToWebView("https://hoitho.tistory.com/1")
            }

            settingTermsOfService.setOnClickListener {
                navigateToWebView("https://hoitho.tistory.com/2")
            }

            settingTermOfLocation.setOnClickListener {
                navigateToWebView("https://hoitho.tistory.com/3")
            }

            settingWithdrawal.setOnSingleClickListener {
                handleWithdrawal()
            }
        }
    }

    private fun handleLogout() {
        if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
            return
        }

        if (settingViewModel.isNaverUser()) {
            try {
                NaverIdLoginSDK.logout()
                auth.signOut()
                settingViewModel.onLogoutSuccess()
            } catch (e: Exception) {
                failLogout(e.message.toString(), "Naver")
            }
        } else {
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    failLogout(error.message.toString(), "Kakao")
                } else {
                    auth.signOut()
                    settingViewModel.onLogoutSuccess()
                }
            }
        }
    }

    private fun handleWithdrawal() {
        if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
            return
        }

        val email = settingViewModel.getEmail()
        val password = settingViewModel.getPassword()

        val credential = EmailAuthProvider.getCredential(email, password)
        auth.currentUser?.reauthenticate(credential)?.addOnSuccessListener {
            showWithdrawalDialog(email)
        }?.addOnFailureListener {
            toastMsg("앱을 껏다 켜주세요!")
        }
    }

    private fun showWithdrawalDialog(email: String) {
        val writeDialog = WriteDialog()
        writeDialog.clickYesListener = WriteDialog.OnClickYesListener { writeText ->
            if (email != writeText) {
                toastMsg("이메일이 일치하지 않습니다.")
                return@OnClickYesListener
            }

            showLoadingFragment()
            processWithdrawal()
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
    }

    private fun processWithdrawal() {
        lifecycleScope.launch {
            if (!settingViewModel.deleteAccount()) {
                toastMsg("탈퇴가 재대로 안됐어요..")
                hideLoadingDialog()
                return@launch
            }

            if (settingViewModel.isNaverUser()) {
                deleteNaverAccount()
            } else {
                deleteKakaoAccount()
            }
        }
    }

    private fun deleteNaverAccount() {
        auth.currentUser?.delete()?.addOnSuccessListener {
            NidOAuthLogin().callDeleteTokenApi(object : OAuthLoginCallback {
                override fun onError(errorCode: Int, message: String) {
                    onFailure(errorCode, message)
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    settingViewModel.onDeleteAccountSuccess()
                    hideLoadingDialog()
                }

                override fun onSuccess() {
                    settingViewModel.onDeleteAccountSuccess()
                    hideLoadingDialog()
                }
            })
        }?.addOnFailureListener {
            toastMsg("탈퇴가 재대로 안됐어요..")
            hideLoadingDialog()
        }
    }

    private fun deleteKakaoAccount() {
        auth.currentUser?.delete()?.addOnSuccessListener {
            UserApiClient.instance.unlink {
                settingViewModel.onDeleteAccountSuccess()
                hideLoadingDialog()
            }
        }?.addOnFailureListener {
            toastMsg("탈퇴가 재대로 안됐어요..")
            hideLoadingDialog()
        }
    }


    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToMyPage() {
        navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
    }

    private fun navigateToLogin() {
        context?.let { ctx ->
            val loginIntent = Intent(ctx, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
        }
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
            lifecycleScope.launch {
                val alarmFunctions = AlarmFunctions(ctx)
                val alarms = settingViewModel.getAllAlarms()

                for (alarm in alarms) {
                    alarmFunctions.cancelAlarm(alarm.alarmCode)
                }

                settingViewModel.deleteAlarms(alarms)
            }
        }
    }

    private fun toastMsg(msg: String) {
        context?.let { ctx ->
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToWebView(uri: String) {
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