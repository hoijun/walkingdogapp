package com.example.walkingdogapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.album.AlbumMapFragment
import com.example.walkingdogapp.collection.CollectionFragment
import com.example.walkingdogapp.databinding.ActivityMainBinding
import com.example.walkingdogapp.mainhome.HomeFragment
import com.example.walkingdogapp.mypage.ManageDogsFragment
import com.example.walkingdogapp.mypage.MyPageFragment
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkLatLng
import com.example.walkingdogapp.walking.SaveWalkDate
import com.example.walkingdogapp.walking.WalkingActivity
import com.example.walkingdogapp.walking.WalkingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var userInfoViewModel: UserInfoViewModel
    private lateinit var loadingDialogFragment: LoadingDialogFragment
    private val locationPermissionRequestCode = 1000
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private var backPressedTime: Long = 0

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

    companion object { // 다른 액티비티로 변경 시 어떤 프래그먼트에서 변경했는지 
        var preFragment = "Home"
        var dogNameList = mutableListOf<String>()
        var dogUriList = HashMap<String, Uri>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (preFragment == "Mypage") {
            binding.menuBn.selectedItemId = R.id.navigation_mypage
        } // 다른 액티비티로 마이페이지에서 변경 했었을 경우, 다시 되돌아 올 때 바텀바의 표시를 마이페이지로 변경

        // 위치 권한
        ActivityCompat.requestPermissions(this, permissions, locationPermissionRequestCode)

        this.onBackPressedDispatcher.addCallback(this, callback)

        if (isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            startActivity(walkingIntent)
        }

        loadingDialogFragment = LoadingDialogFragment()
        loadingDialogFragment.show(this.supportFragmentManager, "loading")

        userInfoViewModel = ViewModelProvider(this).get(UserInfoViewModel::class.java)

        getUserInfo()

        // 화면 전환
        binding.menuBn.apply {
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> {
                        changeFragment(HomeFragment())
                        true
                    }

                    R.id.navigation_collection -> {
                        changeFragment(CollectionFragment())
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

            setOnItemReselectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> { }

                    R.id.navigation_collection -> { }

                    R.id.navigation_albummap -> { }

                    else -> { }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    userInfoViewModel.getLastLocation()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getUserInfo() {
        userInfoViewModel.successGetData.observe(this) {
            try {
                loadingDialogFragment.dismiss()
                dogUriList = userInfoViewModel.dogsImg.value ?: hashMapOf()
            } catch (_: Exception) {

            } finally {
                when (preFragment) {
                    "Mypage" -> {
                        changeFragment(MyPageFragment())
                    }

                    "Home" -> {
                        changeFragment(HomeFragment())
                    }

                    else -> {
                        changeFragment(ManageDogsFragment())
                    }
                }
            }
        }
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.screenFl.id, fragment)
            .commitAllowingStateLoss()
        binding.menuBn.visibility = View.VISIBLE
    }

    // 산책 진행 여부
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