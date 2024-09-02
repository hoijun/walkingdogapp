package com.example.walkingdogapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.walkingdogapp.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
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
        installSplashScreen()

        setContentView(R.layout.activity_splash)
        this.onBackPressedDispatcher.addCallback(this, callback)
        auth = FirebaseAuth.getInstance()

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            signInFirebase()
        }
    }

    private fun signInFirebase() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                startLogin()
                return
            }

            currentUser.getIdToken(true).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    auth.signOut()
                    startLogin()
                    return@addOnCompleteListener
                }

                startMain()
            }
        } catch (e: Exception) {
            startLogin()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
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