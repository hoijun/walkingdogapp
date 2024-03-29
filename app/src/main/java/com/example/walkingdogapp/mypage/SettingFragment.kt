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
import com.example.walkingdogapp.LoginActivity
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.WriteDialog
import com.example.walkingdogapp.databinding.FragmentSettingBinding
import com.example.walkingdogapp.userinfo.DogInfo
import com.example.walkingdogapp.userinfo.UserInfo
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.oauth.api.NidOAuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var userdogInfo: DogInfo
    private lateinit var userInfo: UserInfo
    private lateinit var mainactivity: MainActivity

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var db = FirebaseDatabase.getInstance()

    private lateinit var loginInfo: android.content.SharedPreferences

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMypage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        userdogInfo = myViewModel.doginfo.value ?: DogInfo()
        userInfo = myViewModel.userinfo.value ?: UserInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.apply {
            btnGoMypage.setOnClickListener {
                goMypage()
            }

            dogname.text = "${userdogInfo.name} 보호자"
            username.text = "${userInfo.name} 님"

            logoutbtn.setOnClickListener {
                if (userInfo.email.contains("naver.com")) { // 네이버로 로그인 했을 경우
                    try {
                        NaverIdLoginSDK.logout()
                        auth.signOut()
                        goLogin()
                        Toast.makeText(requireContext(), "로그아웃 성공", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "로그아웃 실패 $e", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (userInfo.email.contains("kakao.com")) { // 카카오로 로그인 했을 경우
                    UserApiClient.instance.logout { error ->
                        if (error != null) {
                            Toast.makeText(
                                requireContext(),
                                "로그아웃 실패 $error",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            auth.signOut()
                            goLogin()
                            Toast.makeText(requireContext(), "로그아웃 성공", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

            settingAlarm.setOnClickListener {

            }

            settingInquiry.setOnClickListener {

            }

            settingAppinfo.setOnClickListener {

            }

            settingTermsofservice.setOnClickListener {

            }

            settingWithdrawal.setOnClickListener {
                val writeDialog = WriteDialog()
                writeDialog.clickYesListener = WriteDialog.OnClickYesListener { writeText ->
                    if (userInfo.email == writeText) { // 이메일 올바르게 입력
                        lifecycleScope.launch {
                            if (userInfo.email.contains("naver.com")) { // 네이버로 로그인 했을 경우
                                binding.settingscreen.visibility = View.INVISIBLE
                                binding.waitImage.visibility = View.VISIBLE
                                if (removeUserInfo()) { // 유저 정보가 올바르게 지워 졌을 경우
                                    try {
                                        auth.currentUser?.delete()
                                    } catch (e: Exception) {
                                        goLogin()
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
                                            goLogin()
                                        }

                                        override fun onSuccess() {
                                            goLogin()
                                        }
                                    })
                                } else { // 유저 정보가 올바르게 지워지지 않았을 경우
                                    Toast.makeText(
                                        requireContext(),
                                        "탈퇴가 재대로 안됐어요..",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding.settingscreen.visibility = View.VISIBLE
                                    binding.waitImage.visibility = View.INVISIBLE
                                }
                            } else if (userInfo.email.contains("kakao.com")) { // 카카오로 로그인 했을 경우
                                binding.settingscreen.visibility = View.INVISIBLE
                                binding.waitImage.visibility = View.VISIBLE
                                if (removeUserInfo()) {
                                    try {
                                        auth.currentUser?.delete()
                                    } catch (e: Exception) {
                                        goLogin()
                                    }
                                    UserApiClient.instance.unlink { error ->
                                        goLogin()
                                    }
                                } else { // 유저 정보가 올바르게 지워지지 않았을 경우
                                    Toast.makeText(
                                        requireContext(),
                                        "탈퇴가 재대로 안됐어요..",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding.settingscreen.visibility = View.VISIBLE
                                    binding.waitImage.visibility = View.INVISIBLE
                                }
                            }
                        }
                    } else { // 입력한 값과 이메일이 같지 않았을 경우
                        Toast.makeText(
                            requireContext(),
                            "정확한 이메일을 입력해 주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun goMypage() {
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

    private suspend fun removeUserInfo(): Boolean {
        var error = false
        val uid = auth.currentUser?.uid
        val storgeRef = storage.getReference("$uid").child("images").child("profileimg")
        val userRef = db.getReference("Users").child("$uid")
        val result = lifecycleScope.async(Dispatchers.IO) {
            val deleteprofileJob = async(Dispatchers.IO) {
                try {
                    storgeRef.delete().await()
                } catch (e: Exception) {
                    if (e.message.toString().contains("Object does not exist at location.")) {
                        error = false
                    } else {
                        Log.d("savepoint", e.message.toString())
                        error = true
                    }
                }
            }

            deleteprofileJob.await()

            if(error) {
                return@async false
            }

            val deleteInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.removeValue().await()
                } catch (e: Exception) {
                    Log.d("savepoint", e.message.toString())
                    error = true
                }
            }

            deleteInfoJob.await()

            return@async !error
        }
        return result.await()
    }
}