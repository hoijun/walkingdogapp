package com.example.walkingdogapp.registerinfo

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.health.connect.datatypes.units.Length
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.LoadingDialogFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.databinding.ActivityRegisterUserBinding
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegisterUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterUserBinding
    private lateinit var userInfoViewModel: UserInfoViewModel
    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectGoMain()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.onBackPressedDispatcher.addCallback(this, backPressCallback)

        userInfoViewModel = ViewModelProvider(this).get(UserInfoViewModel::class.java)

        val currentUserInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("userinfo", UserInfo::class.java)
        } else {
            intent.getSerializableExtra("userinfo") as UserInfo?
        }

        val userInfo = currentUserInfo ?: UserInfo()

        binding.apply {
            user = userInfo

            btnUserisfemale.setOnClickListener {
                userInfo.gender = "여"
                user = userInfo
            }

            btnUserismale.setOnClickListener {
                userInfo.gender = "남"
                user = userInfo
            }

            editBirth.setOnClickListener {
                val cal = Calendar.getInstance()
                val dateCallback = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val birth = "${year}/${month + 1}/${day}"
                    if (Constant.getAge(birth) == -1) {
                        Toast.makeText(
                            this@RegisterUserActivity,
                            "올바른 생일을 입력 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnDateSetListener
                    }
                    userInfo.birth = birth
                    user = userInfo
                }

                val datePicker = DatePickerDialog(
                    this@RegisterUserActivity,
                    dateCallback,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                datePicker.datePicker.maxDate = cal.timeInMillis
                datePicker.show()
            }

            registerUser.setOnClickListener {
                if(!NetworkManager.checkNetworkState(this@RegisterUserActivity)) {
                    return@setOnClickListener
                }
                userInfo.apply {
                    if (editName.text.toString() == "" || birth == "" || gender == "") {
                        val builder = AlertDialog.Builder(this@RegisterUserActivity)
                        builder.setTitle("빈칸이 남아있어요.")
                        builder.setPositiveButton("확인", null)
                        builder.show()
                        return@setOnClickListener
                    }
                }

                val builder = AlertDialog.Builder(this@RegisterUserActivity)
                builder.setTitle("등록 할까요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val loadingDialogFragment = LoadingDialogFragment()
                            loadingDialogFragment.show(this@RegisterUserActivity.supportFragmentManager, "loading")
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    userInfoViewModel.updateUserInfo(userInfo)
                                    withContext(Dispatchers.Main) {
                                        loadingDialogFragment.dismiss()
                                        goHome()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        loadingDialogFragment.dismiss()
                                    }
                                }
                            }
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
                return@setOnClickListener
            }

            btnBack.setOnClickListener {
                selectGoMain()
            }
        }
    }

    private fun selectGoMain() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("나가시겠어요?")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    goHome()
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니요", null)
        builder.show()
    }

    private fun goHome() {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(backIntent)
        finish()
    }

    object RegisterUserBindingAdapter {
        @BindingAdapter("userGender")
        @JvmStatic
        fun setBackground(btn: Button, gender: String) {
            if(gender == btn.text) {
                btn.setBackgroundColor(Color.parseColor("#ff444444"))
            } else {
                btn.setBackgroundColor(Color.parseColor("#ff888888"))
            }
        }
        @BindingAdapter("userNameLength")
        @JvmStatic
        fun setSelection(editText: EditText, length: Int) {
            editText.setSelection(length)
        }
    }
}