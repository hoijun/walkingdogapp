package com.tulmunchi.walkingdogapp.presentation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.databinding.ActivityMainBinding
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.ui.album.AlbumMapFragment
import com.tulmunchi.walkingdogapp.presentation.ui.collection.CollectionFragment
import com.tulmunchi.walkingdogapp.presentation.ui.home.HomeFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage.ManageDogsFragment
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingService
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val locationPermissionRequestCode = 1000
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )
    private var backPressedTime: Long = 0

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private var loadingDialog: LoadingDialog? = null

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
        var dogImageUrls = HashMap<String, String>()  // Uri → String (URL)
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

        loadingDialog = loadingDialogFactory.create(supportFragmentManager)

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

        // 위치 권한
        permissionHandler.requestPermissions(this, permissions, locationPermissionRequestCode)


        this.onBackPressedDispatcher.addCallback(this, callback)

        // 산책 중인지 확인
        if (WalkingService.isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            walkingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(walkingIntent)
        }

        showLoadingFragment()
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
        setupViewModelObservers(isImgChanged)
        mainViewModel.loadUserData(loadImages = isImgChanged)
    }

    private fun setupViewModelObservers(isImgChanged: Boolean) {
        // 로딩 상태 관찰
        mainViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoadingFragment() else hideLoadingDialog()
        }

        // 에러 관찰
        mainViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                mainViewModel.clearError()
            }
        }

        // 데이터 로드 성공 관찰
        mainViewModel.dataLoadSuccess.observe(this) { success ->
            if (success) {
                try {
                    // 강아지 이름 목록 저장
                    dogNameList = mainViewModel.dogNames.value ?: listOf()

                    // 강아지 이미지 URL 저장
                    if (isImgChanged) dogImageUrls = HashMap(mainViewModel.dogImages.value ?: emptyMap())

                } catch (e: Exception) {
                    Toast.makeText(this, "데이터 로드 중 오류 발생", Toast.LENGTH_SHORT).show()
                } finally {
                    val targetFragment = when (preFragment) {
                        "Mypage" -> MyPageFragment()
                        "Home" -> HomeFragment()
                        "Manage" -> ManageDogsFragment()
                        "Collection" -> CollectionFragment()
                        "AlbumMap" -> AlbumMapFragment()
                        else -> HomeFragment()
                    }
                    changeFragment(targetFragment)
                }
            }
        }
    }

    private fun showLoadingFragment() {
        if (isFinishing || isDestroyed) {
            return
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        if (isFinishing || isDestroyed) {
            return
        }
        loadingDialog?.dismiss()
    }

    fun changeFragment(fragment: Fragment) {
        if (!::binding.isInitialized) {
            return
        }

        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(binding.screenFl.id, fragment)
            .commitAllowingStateLoss()
    }
}