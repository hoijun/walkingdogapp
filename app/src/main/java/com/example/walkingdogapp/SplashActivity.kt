package com.example.walkingdogapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var loginInfo: android.content.SharedPreferences
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
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