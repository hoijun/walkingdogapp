package com.tulmunchi.walkingdogapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.tulmunchi.walkingdogapp.album.AlbumMapFragment
import com.tulmunchi.walkingdogapp.collection.CollectionFragment
import com.tulmunchi.walkingdogapp.databinding.ActivityMainBinding
import com.tulmunchi.walkingdogapp.mainhome.HomeFragment
import com.tulmunchi.walkingdogapp.mypage.ManageDogsFragment
import com.tulmunchi.walkingdogapp.mypage.MyPageFragment
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.walking.WalkingActivity
import com.tulmunchi.walkingdogapp.walking.WalkingService
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var loadingDialogFragment: LoadingDialogFragment
    private val locationPermissionRequestCode = 1000
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
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
        var dogNameList = listOf<String>()
        var dogUriList = HashMap<String, Uri>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (preFragment == "Mypage" || preFragment == "Manage") {
            binding.menuBn.selectedItemId = R.id.navigation_mypage
        } // 다른 액티비티로 마이페이지에서 변경 했었을 경우, 다시 되돌아 올 때 바텀바의 표시를 마이페이지로 변경

        if (preFragment =="Collection")
            binding.menuBn.selectedItemId = R.id.navigation_collection

        if (preFragment =="AlbumMap")
            binding.menuBn.selectedItemId = R.id.navigation_albummap

        // 위치 권한
        ActivityCompat.requestPermissions(this, permissions, locationPermissionRequestCode)

        this.onBackPressedDispatcher.addCallback(this, callback)

        if (WalkingService.isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            walkingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(walkingIntent)
        }

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

        loadingDialogFragment = LoadingDialogFragment()
        loadingDialogFragment.show(this.supportFragmentManager, "loading")

        getUserInfo()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mainViewModel.getLastLocation()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun setMenuVisibility(visibility: Int) {
        if (::binding.isInitialized) {
            binding.menuBn.visibility = visibility
        }
    }

    private fun getUserInfo() {
        if (!::binding.isInitialized) {
            Toast.makeText(this, "앱을 재시작 해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val isImgChanged = intent.getBooleanExtra("isImgChanged", true)
        mainViewModel.observeUser(isImgChanged)
        mainViewModel.successGetData.observe(this) {
            try {
                loadingDialogFragment.dismiss()
                dogNameList = mainViewModel.dogsNames.value ?: listOf()
                if (mainViewModel.isSuccessGetImg()) {
                    dogUriList = mainViewModel.dogsImg.value ?: hashMapOf()
                } else {
                    mainViewModel.setDogsImg(dogUriList)
                }
            } catch (_: Exception) { }
            finally {
               val targetFragment = when (preFragment) {
                    "Mypage" -> {
                        MyPageFragment()
                    }

                    "Home" -> {
                        HomeFragment()
                    }

                    "Manage" -> {
                        ManageDogsFragment()
                    }

                    "Collection" -> {
                        CollectionFragment()
                    }

                    "AlbumMap" -> {
                        AlbumMapFragment()
                    }

                    else -> {
                        HomeFragment()
                    }
                }

                changeFragment(targetFragment)
            }
        }
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.screenFl.id, fragment)
            .commitAllowingStateLoss()
    }
}