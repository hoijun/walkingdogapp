package com.example.walkingdogapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.walkingdogapp.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainviewmodel: LocateInfoViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private var backPressedTime : Long = 0

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backPressedTime < 2500) {
                moveTaskToBack(true)
                finishAndRemoveTask()
                exitProcess(0)
            }
            Toast.makeText(this@MainActivity, "한번 더 클릭 시 종료 됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 위치 권한
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)

        this.onBackPressedDispatcher.addCallback(this, callback)

        if (isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            startActivity(walkingIntent)
        }
        // 보류
        mainviewmodel = ViewModelProvider(this).get(LocateInfoViewModel::class.java)

        // 화면 전환
        binding.menuBn.run {
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> {
                        changeFragment(HomeFragment())
                        true
                    }

                    R.id.navigation_book -> {
                        changeFragment(BookFragment())
                        true
                    }

                    R.id.navigation_albummap -> {
                        changeFragment(AlbumMapFragment())
                        true
                    }

                    else -> {
                        changeFragment(MyPageFragment())
                        true
                    }
                }
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.screenFl.id, fragment)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("onRequest", "onRequestPermissionsResult")
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("권한 승인", "권한 승인됨")
                    mainviewmodel.getLastLocation()
                    changeFragment(HomeFragment())
                } else {
                    Log.d("권한 거부", "권한 거부됨")
                    changeFragment(HomeFragment())
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isWalkingServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (myService in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (WalkingService::class.java.name == myService.service.className) {
                if (myService.foreground) {
                    return true
                }
            }
            return false
        }
        return false
    }
}
