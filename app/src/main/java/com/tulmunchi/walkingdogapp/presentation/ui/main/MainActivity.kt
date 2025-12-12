package com.tulmunchi.walkingdogapp.presentation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.ActivityMainBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.ui.alarm.SettingAlarmFragment
import com.tulmunchi.walkingdogapp.presentation.ui.album.AlbumMapFragment
import com.tulmunchi.walkingdogapp.presentation.ui.collection.CollectionFragment
import com.tulmunchi.walkingdogapp.presentation.ui.gallery.detailOfPicturePage.DetailPictureFragment
import com.tulmunchi.walkingdogapp.presentation.ui.gallery.galleryPage.GalleryFragment
import com.tulmunchi.walkingdogapp.presentation.ui.home.HomeFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.dogInfoPafge.DogInfoFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage.ManageDogsFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.settingPage.SettingFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.detailWalkInfoPage.DetailWalkInfoFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.walkInfoWithCalendarPage.WalkInfoFragment
import com.tulmunchi.walkingdogapp.presentation.ui.register.registerDogPage.RegisterDogFragment
import com.tulmunchi.walkingdogapp.presentation.ui.register.registerUserPage.RegisterUserFragment
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingActivity
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingService
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    private var loadingDialog: LoadingDialog? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = loadingDialogFactory.create(supportFragmentManager)

        if (savedInstanceState == null) {
            // NavigationManager 초기 상태 설정 (로그아웃/탈퇴 후 재진입 시 Home으로 시작)
            navigationManager.navigateTo(NavigationState.WithBottomNav.Home)
            setupNavigation()
            setupBottomNavigation()
            requestPermissions()
            checkWalkingService()
            loadUserData()
        }

        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupNavigation() {
        navigationManager.currentState.observe(this) { state ->
            val fragment = createFragmentForState(state)
            changeFragment(fragment)
            updateBottomNavigationVisibility(state)
            updateBottomNavigationSelection(state)
        }
    }

    private fun createFragmentForState(state: NavigationState): Fragment {
        return when (state) {
            // WithBottomNav
            is NavigationState.WithBottomNav.Home -> HomeFragment()
            is NavigationState.WithBottomNav.MyPage -> MyPageFragment()
            is NavigationState.WithBottomNav.Collection -> CollectionFragment()
            is NavigationState.WithBottomNav.AlbumMap -> AlbumMapFragment()

            // WithoutBottomNav - Register
            is NavigationState.WithoutBottomNav.RegisterDog -> {
                RegisterDogFragment().apply {
                    arguments = Bundle().apply {
                        state.dog?.let { putSerializable("dogInfo", it) }
                        putString("from", state.from)
                    }
                }
            }

            is NavigationState.WithoutBottomNav.RegisterUser -> {
                RegisterUserFragment().apply {
                    arguments = Bundle().apply {
                        putString("from", state.from)
                    }
                }
            }

            // WithoutBottomNav - MyPage 하위
            is NavigationState.WithoutBottomNav.ManageDogs -> ManageDogsFragment()

            is NavigationState.WithoutBottomNav.DogInfo -> {
                DogInfoFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("dogInfo", state.dog)
                        putString("before", state.before)
                    }
                }
            }

            is NavigationState.WithoutBottomNav.WalkInfo -> {
                WalkInfoFragment().apply {
                    if (state.selectDateRecord != null && state.selectDog != null) {
                        arguments = Bundle().apply {
                            putStringArrayList("selectDateRecord", ArrayList(state.selectDateRecord))
                            putSerializable("selectDog", state.selectDog)
                        }
                    }
                }
            }

            is NavigationState.WithoutBottomNav.DetailWalkInfo -> {
                DetailWalkInfoFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("selectDateRecord", state.walkRecord)
                        putSerializable("selectDog", state.dog)
                    }
                }
            }

            // WithoutBottomNav - Gallery
            is NavigationState.WithoutBottomNav.Gallery -> {
                GalleryFragment().apply {
                    arguments = Bundle().apply {
                        state.currentImgIndex?.let { putInt("currentImgIndex", it) }
                    }
                }
            }

            is NavigationState.WithoutBottomNav.DetailPicture -> {
                DetailPictureFragment().apply {
                    arguments = Bundle().apply {
                        putInt("select", state.selectImageIndex)
                    }
                }
            }

            // WithoutBottomNav - 기타
            is NavigationState.WithoutBottomNav.SettingAlarm -> SettingAlarmFragment()
            is NavigationState.WithoutBottomNav.Setting -> SettingFragment()
        }
    }

    private fun setupBottomNavigation() {
        binding.menuBn.setOnItemSelectedListener { item ->
            val state = when (item.itemId) {
                R.id.navigation_home -> NavigationState.WithBottomNav.Home
                R.id.navigation_collection -> NavigationState.WithBottomNav.Collection
                R.id.navigation_albummap -> NavigationState.WithBottomNav.AlbumMap
                R.id.navigation_mypage -> NavigationState.WithBottomNav.MyPage
                else -> return@setOnItemSelectedListener false
            }
            navigationManager.navigateTo(state)
            true
        }

        binding.menuBn.setOnItemReselectedListener {
            // 재선택 시 아무 동작 안 함
        }
    }

    private fun updateBottomNavigationVisibility(state: NavigationState) {
        binding.menuBn.isVisible = state is NavigationState.WithBottomNav
    }

    private fun updateBottomNavigationSelection(state: NavigationState) {
        if (state !is NavigationState.WithBottomNav) return

        val itemId = when (state) {
            is NavigationState.WithBottomNav.Home -> R.id.navigation_home
            is NavigationState.WithBottomNav.Collection -> R.id.navigation_collection
            is NavigationState.WithBottomNav.AlbumMap -> R.id.navigation_albummap
            is NavigationState.WithBottomNav.MyPage -> R.id.navigation_mypage
        }

        if (binding.menuBn.selectedItemId != itemId) {
            binding.menuBn.selectedItemId = itemId
        }
    }

    private fun requestPermissions() {
        permissionHandler.requestPermissions(this, permissions, locationPermissionRequestCode)
    }

    private fun checkWalkingService() {
        if (WalkingService.isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            walkingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(walkingIntent)
        }
    }

    private fun loadUserData() {
        if (!::binding.isInitialized) {
            Toast.makeText(this, "앱을 재시작 해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        setupViewModelObservers()
        mainViewModel.loadUserData()
    }

    private fun setupViewModelObservers() {
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
                // NavigationManager가 자동으로 현재 상태 유지
                navigationManager.navigateTo(navigationManager.currentState.value ?: NavigationState.WithBottomNav.Home)
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
                    mainViewModel.getLastLocation()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 산책에서 돌아올 때 데이터 갱신
        val shouldRefreshData = intent.getBooleanExtra("shouldRefreshData", false)
        if (shouldRefreshData) {
            // 이미지는 유지하고 산책 기록 등 다른 데이터만 업데이트
            mainViewModel.loadUserData(loadImages = false)
        }
    }

    private fun changeFragment(fragment: Fragment) {
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

    // companion object 완전 제거 ✅
}
