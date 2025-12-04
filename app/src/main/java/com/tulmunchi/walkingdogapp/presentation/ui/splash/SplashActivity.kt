package com.tulmunchi.walkingdogapp.presentation.ui.splash

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.core.analytics.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.presentation.ui.login.LoginActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
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

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    @Inject
    lateinit var networkChecker: NetworkChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_splash)
        auth = FirebaseAuth.getInstance()
    }

    override fun onResume() {
        super.onResume()
        checkUpdate(this)
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun checkUpdate(context: Context) {
        if (!networkChecker.isNetworkAvailable()) {
            AlertDialog.Builder(context)
                .setTitle("네트워크 오류")
                .setMessage("네트워크 연결을 확인해주세요.\n업데이트 확인을 위해 네트워크 연결이 필요합니다.")
                .setPositiveButton("재시도") { _, _ ->
                    checkUpdate(context)
                }
                .setNegativeButton("종료") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }

        val appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                AlertDialog.Builder(context)
                    .setTitle("업데이트")
                    .setMessage("최신 버전으로 업데이트 해주세요!")
                    .setPositiveButton("확인") { _, _ ->
                        updateApp(context)
                    }.setCancelable(false)
                    .show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    signInFirebase()
                }
            }
        }.addOnFailureListener {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                signInFirebase()
            }
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
                    firebaseHelper.logEvent(
                        listOf(
                            "type" to "Login_Fail",
                            "api" to "Firebase",
                            "reason" to task.exception?.message.toString()
                        )
                    )

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

    private fun updateApp(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                this.setData("market://details?id=${context.packageName}".toUri())
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            )
            startActivity(webIntent)
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