package com.example.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.LoadingDialogFragment
import com.example.walkingdogapp.LoginActivity
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.WriteDialog
import com.example.walkingdogapp.alarm.AlarmFunctions
import com.example.walkingdogapp.databinding.FragmentSettingBinding
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val userDataViewModel: UserInfoViewModel by activityViewModels()
    private lateinit var user: UserInfo
    private lateinit var mainactivity: MainActivity
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val auth = FirebaseAuth.getInstance()

    private lateinit var loginInfo: android.content.SharedPreferences

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        user = userDataViewModel.userInfo.value ?: UserInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.apply {
            btnGoMypage.setOnClickListener {
                goMyPage()
            }

            userInfo = user

            logoutbtn.setOnClickListener {
                if (user.email.contains("naver.com")) { // 네이버로 로그인 했을 경우
                    try {
                        NaverIdLoginSDK.logout()
                        auth.signOut()
                        goLogin()
                        removeAlarms()
                        Toast.makeText(requireContext(), "로그아웃 성공", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "로그아웃 실패 $e", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (user.email.contains("kakao.com")) { // 카카오로 로그인 했을 경우
                    UserApiClient.instance.logout { error ->
                        if (error != null) {
                            Toast.makeText(
                                requireContext(),
                                "로그아웃 실패 $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            auth.signOut()
                            goLogin()
                            removeAlarms()
                            Toast.makeText(requireContext(), "로그아웃 성공", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

            settingInquiry.setOnClickListener {

            }

            settingPrivacyPolicy.setOnClickListener {

            }

            settingTermsofservice.setOnClickListener {

            }

            settingWithdrawal.setOnClickListener {
                val writeDialog = WriteDialog()
                writeDialog.clickYesListener = WriteDialog.OnClickYesListener { writeText ->
                    if (user.email != writeText) {
                        Toast.makeText(
                            requireContext(),
                            "정확한 이메일을 입력해 주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnClickYesListener
                    } // 이메일 올바르게 입력x

                    val loadingDialogFragment = LoadingDialogFragment()
                    loadingDialogFragment.show(requireActivity().supportFragmentManager, "loading")

                    lifecycleScope.launch {
                        if (user.email.contains("naver.com")) { // 네이버로 로그인 했을 경우
                            if (userDataViewModel.removeAccount()) { // 유저 정보가 올바르게 지워 졌을 경우
                                try {
                                    auth.currentUser?.delete()
                                } catch (e: Exception) {
                                    completeDeleteAccount()
                                    loadingDialogFragment.dismiss()
                                }

                                NidOAuthLogin().callDeleteTokenApi(object :
                                    OAuthLoginCallback {
                                    override fun onError(errorCode: Int, message: String) {
                                        onFailure(errorCode, message)
                                    }

                                    override fun onFailure(
                                        httpStatus: Int,
                                        message: String
                                    ) {
                                        completeDeleteAccount()
                                        loadingDialogFragment.dismiss()
                                    }

                                    override fun onSuccess() {
                                        completeDeleteAccount()
                                        loadingDialogFragment.dismiss()
                                    }
                                })
                            } else { // 유저 정보가 올바르게 지워지지 않았을 경우
                                Toast.makeText(
                                    requireContext(),
                                    "탈퇴가 재대로 안됐어요..",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadingDialogFragment.dismiss()
                            }
                        } else if (user.email.contains("kakao.com")) { // 카카오로 로그인 했을 경우
                            if (userDataViewModel.removeAccount()) {
                                try {
                                    auth.currentUser?.delete()
                                } catch (e: Exception) {
                                    completeDeleteAccount()
                                    loadingDialogFragment.dismiss()
                                }
                                UserApiClient.instance.unlink {
                                    completeDeleteAccount()
                                    loadingDialogFragment.dismiss()
                                }
                            } else { // 유저 정보가 올바르게 지워지지 않았을 경우
                                Toast.makeText(
                                    requireContext(),
                                    "탈퇴가 재대로 안됐어요..",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadingDialogFragment.dismiss()
                            }
                        }
                    }
                }
                val bundle = Bundle()
                bundle.putString("text", "이메일을 입력해주세요.")
                writeDialog.arguments = bundle
                writeDialog.show(requireActivity().supportFragmentManager, "writeemail")
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goMyPage() {
        mainactivity.changeFragment(MyPageFragment())
    }

    private fun removeLoginInfo() {
        loginInfo = mainactivity.getSharedPreferences("setting", AppCompatActivity.MODE_PRIVATE)
        val editor = loginInfo.edit()
        editor.remove("id")
        editor.remove("password")
        editor.apply()
    }

    private fun goLogin() {
        removeLoginInfo()
        val loginIntent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(loginIntent)
        MainActivity.preFragment = "Home" // 로그아웃 바로 후에 홈 부터 시작하기 위함
    }

    private fun removeAlarms() {
        val alarmFunctions = AlarmFunctions(requireContext())
        coroutineScope.launch {
            for (alarm in userDataViewModel.getAlarmList()) {
                alarmFunctions.cancelAlarm(alarm.alarm_code)
                userDataViewModel.deleteAlarm(alarm)
            }
        }
    }

    private fun completeDeleteAccount() {
        removeAlarms()
        goLogin()
    }
}