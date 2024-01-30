package com.example.walkingdogapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var loginInfo: android.content.SharedPreferences
    private lateinit var auth: FirebaseAuth
    private var backPressedTime : Long = 0
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backPressedTime < 2500) {
                moveTaskToBack(true)
                finishAndRemoveTask()
                exitProcess(0)
            }
            Toast.makeText(this@SplashActivity, "한번 더 클릭 시 종료 됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        this.onBackPressedDispatcher.addCallback(this, callback)

        auth = FirebaseAuth.getInstance()

        loginInfo = getSharedPreferences("setting", MODE_PRIVATE)
        val loginId = loginInfo.getString("id", null)
        val loginPassword = loginInfo.getString("password", null)
        if (loginId != null && loginPassword != null) {
            signinFirebase(loginId, loginPassword)
        }
        else {
            startLogin()
        }
    }

    private fun signinFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {  //통신 완료가 된 후 무슨일을 할지
                    task ->
                if (task.isSuccessful) {
                    //로그인 처리를 해주면 됨!
                    if (auth.currentUser?.uid != null) {
                        saveUid(auth.currentUser?.uid!!)
                    }
                    startMain()
                } else {
                    // 오류가 난 경우!
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUid(uid : String) {
        val editor = loginInfo.edit()
        editor.putString("uid",uid)
        editor.apply()
    }

    private fun startMain() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
    }

    private fun startLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
    }
}