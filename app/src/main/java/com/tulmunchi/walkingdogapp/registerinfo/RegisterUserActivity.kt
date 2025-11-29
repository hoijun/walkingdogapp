package com.tulmunchi.walkingdogapp.registerinfo

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.lifecycle.lifecycleScope
import com.tulmunchi.walkingdogapp.utils.Utils
import com.tulmunchi.walkingdogapp.common.LoadingDialogFragment
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.ActivityRegisterUserBinding
import javax.inject.Inject
import com.tulmunchi.walkingdogapp.datamodel.UserInfo
import com.tulmunchi.walkingdogapp.viewmodel.RegisterUserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.core.graphics.toColorInt

@AndroidEntryPoint
class RegisterUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterUserBinding
    private val registerUserViewModel: RegisterUserViewModel by viewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

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
                    if (Utils.getAge(birth) == -1) {
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
                if(!networkChecker.isNetworkAvailable()) {
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
                                    registerUserViewModel.updateUserInfo(userInfo)
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
        val backIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("isImgChanged", false)
        }
        startActivity(backIntent)
        finish()
    }

    object RegisterUserBindingAdapter {
        @BindingAdapter("userGender")
        @JvmStatic
        fun setBackground(btn: Button, gender: String) {
            val color = if (gender == btn.text.toString()) "#ff444444" else "#ff888888"
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * btn.resources.displayMetrics.density  // 10dp를 픽셀로 변환
                setColor(color.toColorInt())
            }
            btn.background = shape
        }

        @BindingAdapter("userNameLength")
        @JvmStatic
        fun setSelection(editText: EditText, length: Int) {
            editText.setSelection(length)
        }
    }
}