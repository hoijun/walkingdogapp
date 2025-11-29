package com.tulmunchi.walkingdogapp.mypage

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
import com.tulmunchi.walkingdogapp.common.LoadingDialogFragment
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.common.WriteDialog
import com.tulmunchi.walkingdogapp.alarm.AlarmFunctions
import com.tulmunchi.walkingdogapp.databinding.FragmentSettingBinding
import com.tulmunchi.walkingdogapp.login.LoginActivity
import com.tulmunchi.walkingdogapp.utils.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
            val sharedPreferences = it.getSharedPreferences("UserEmail", Context.MODE_PRIVATE)
            email = sharedPreferences.getString("email", "") ?: ""
            password = sharedPreferences.getString("password", "") ?: ""
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.apply {
            btnGoMypage.setOnClickListener {
                goMyPage()
            }

            userEmail = email

            logoutBtn.setOnClickListener {
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }
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
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }
                }

                val loadingDialogFragment = LoadingDialogFragment()
                parentFragmentManager.let {
                    try {
                        loadingDialogFragment.show(it, "loading")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val credential = EmailAuthProvider.getCredential(email, password)
                auth.currentUser?.reauthenticate(credential)?.addOnSuccessListener {
                    loadingDialogFragment.dismiss()
                    val writeDialog = WriteDialog()
                    writeDialog.clickYesListener = WriteDialog.OnClickYesListener { writeText ->
                        if (userEmail != writeText) {
                            toastMsg("이메일이 일치하지 않습니다.")
                            return@OnClickYesListener
                        } // 이메일 올바르게 입력x

                        parentFragmentManager.let {
                            try {
                                loadingDialogFragment.show(it, "loading")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        lifecycleScope.launch {
                            if (email.contains("@naver.com")) { // 네이버로 로그인 했을 경우
                                if (!mainViewModel.removeAccount()) {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    loadingDialogFragment.dismiss()
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
                                            loadingDialogFragment.dismiss()
                                        }

                                        override fun onSuccess() {
                                            completeDeleteAccount()
                                            loadingDialogFragment.dismiss()
                                        }
                                    })
                                }?.addOnFailureListener {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    loadingDialogFragment.dismiss()
                                }
                            } else { // 카카오로 로그인 했을 경우
                                if (!mainViewModel.removeAccount()) {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    loadingDialogFragment.dismiss()
                                    return@launch
                                }

                                auth.currentUser?.delete()?.addOnSuccessListener {
                                    UserApiClient.instance.unlink {
                                        completeDeleteAccount()
                                        loadingDialogFragment.dismiss()
                                    }
                                }?.addOnFailureListener {
                                    toastMsg("탈퇴가 재대로 안됐어요..")
                                    loadingDialogFragment.dismiss()
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
            MainActivity.preFragment = "Home" // 로그아웃 바로 후에 홈 부터 시작하기 위함
        }
    }

    private fun completeDeleteAccount() {
        removeAlarms()
        goLogin()
    }

    private fun successLogout() {
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
                for (alarm in mainViewModel.getAlarmList()) {
                    alarmFunctions.cancelAlarm(alarm.alarm_code)
                    mainViewModel.deleteAlarm(alarm)
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

}