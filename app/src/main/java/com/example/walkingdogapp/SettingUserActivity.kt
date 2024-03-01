package com.example.walkingdogapp

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.databinding.ActivitySettingUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingUserBinding
    private var userinfo = UserInfo()
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val BackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectgoMain()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.onBackPressedDispatcher.addCallback(this, BackPressCallback)

        binding.apply {
            btnUserisfemale.setOnClickListener {
                userinfo.gender = btnUserisfemale.text.toString()
                btnUserisfemale.setBackgroundColor(Color.DKGRAY)
                btnUserismale.setBackgroundColor(Color.GRAY)
            }

            btnUserismale.setOnClickListener {
                userinfo.gender = btnUserismale.text.toString()
                btnUserismale.setBackgroundColor(Color.DKGRAY)
                btnUserisfemale.setBackgroundColor(Color.GRAY)
            }

            editBirth.setOnClickListener {
                val cal = Calendar.getInstance()
                val dateCallback = DatePickerDialog.OnDateSetListener { view, year, month, day ->
                    val birth = "${year}/${month + 1}/${day}"
                    if (getAge(birth) == -1) {
                        Toast.makeText(this@SettingUserActivity, "올바른 생일을 입력 해주세요!", Toast.LENGTH_SHORT).show()
                        return@OnDateSetListener
                    }
                    userinfo.birth = birth
                    binding.editBirth.text = birth
                }
                DatePickerDialog(this@SettingUserActivity, dateCallback, cal.get(Calendar.YEAR), cal.get(
                    Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }

            registerUser.setOnClickListener {
                if (editName.text.toString() == "" || editBirth.text == "") {
                    val builder = AlertDialog.Builder(this@SettingUserActivity)
                    builder.setTitle("빈칸이 남아있어요.")
                    builder.setPositiveButton("확인", null)
                    builder.show()
                    return@setOnClickListener
                }

                userinfo.name = editName.text.toString()

                val builder = AlertDialog.Builder(this@SettingUserActivity)
                builder.setTitle("등록 할까요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            binding.settingscreen.visibility = View.INVISIBLE
                            binding.waitImage.visibility = View.VISIBLE
                            val uid = auth.currentUser?.uid
                            val userRef = db.getReference("Users").child("$uid").child("user")
                            lifecycleScope.launch {
                                val nameDeferred = async(Dispatchers.IO) {
                                    try {
                                        userRef.child("name").setValue(userinfo.name).await()
                                    } catch (e: Exception) {
                                        return@async
                                    }
                                }

                                val genderDeferred = async(Dispatchers.IO) {
                                    try {
                                        userRef.child("gender").setValue(userinfo.gender).await()
                                    } catch (e: Exception) {
                                        return@async
                                    }
                                }

                                val birthDeferred = async(Dispatchers.IO) {
                                    try {
                                        userRef.child("birth").setValue(userinfo.birth).await()
                                    } catch (e: Exception) {
                                        return@async
                                }}

                                nameDeferred.await()
                                genderDeferred.await()
                                birthDeferred.await()

                                goHome()
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
                selectgoMain()
            }
        }
    }

    private fun getAge(date: String): Int {
        val currentDate = Calendar.getInstance()
        var age = -1

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val birthDate = dateFormat.parse(date)
        val calBirthDate = Calendar.getInstance().apply { time = birthDate }

        if (calBirthDate.time < currentDate.time) {
            age = currentDate.get(Calendar.YEAR) - calBirthDate.get(Calendar.YEAR)
            if (currentDate.get(Calendar.DAY_OF_YEAR) < calBirthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            Log.d("savepoint", age.toString())
        }
        return age
    }

    private fun selectgoMain() {
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
}