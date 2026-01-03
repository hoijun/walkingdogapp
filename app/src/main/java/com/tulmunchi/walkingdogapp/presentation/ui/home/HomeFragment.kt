package com.tulmunchi.walkingdogapp.presentation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentHomeBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.ui.alarm.SettingAlarmFragment
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingActivity
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val selectedDogList = mutableListOf<String>()
    private var homeDogListAdapter: HomeDogListAdapter? = null

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var navigationManager: NavigationManager

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!networkChecker.isNetworkAvailable()) {
            val dialog = SelectDialog.newInstance(title = "인터넷을 연결해주세요!")
            dialog.isCancelable = false
            dialog.show(parentFragmentManager, "networkCheck")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.apply {
            viewmodel = mainViewModel
            lifecycleOwner = viewLifecycleOwner
            selectedDogs = selectedDogList.joinToString(", ")

            refresh.apply {
                this.setOnRefreshListener {
                    mainViewModel.loadUserData()
                }
                mainViewModel.dataLoadSuccess.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            homeDogsViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    refresh.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE)
                }
            })

            btnAlarm.setOnClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable()) {
                        return@setOnClickListener
                    }
                }

                navigateToSettingAlarm()
            }

            val dogsList = mainViewModel.dogs.value ?: listOf()
            val dogImages = mainViewModel.dogImages.value ?: emptyMap()
            homeDogListAdapter = HomeDogListAdapter(dogsList, mainViewModel.isSuccessGetData(), networkChecker, dogImages)
            homeDogListAdapter?.onClickDogListener =
                HomeDogListAdapter.OnClickDogListener { dogName ->
                    if (selectedDogList.contains(dogName)) {
                        selectedDogList.remove(dogName)
                    } else {
                        selectedDogList.add(dogName)
                    }

                    selectedDogs = selectedDogList.joinToString(", ")
                }
            homeDogListAdapter?.onAddDogClickListener =
                HomeDogListAdapter.OnAddDogClickListener {
                    navigationManager.navigateTo(
                        NavigationState.WithoutBottomNav.RegisterDog(
                            dog = null,
                            from = "home"
                        )
                    )
                }

            homeDogsViewPager.adapter = homeDogListAdapter
            TabLayoutMediator(homeDogsIndicator, homeDogsViewPager) { _, _ -> }.attach()

            btnWalk.setOnClickListener {
                handleWalkButtonClick(dogsList)
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeDogListAdapter = null
        _binding = null
    }

    private fun handleWalkButtonClick(dogsList: List<Any>) {
        val ctx = context ?: return

        if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
            return
        }

        if (dogsList.isEmpty()) {
            showRegisterDogDialog(ctx)
            return
        }

        if (selectedDogList.isEmpty()) {
            Toast.makeText(ctx, "함께 산책할 강아지를 선택해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkLocationPermissions()) {
            showPermissionDialog(ctx)
            return
        }

        val intent = Intent(ctx, WalkingActivity::class.java).apply {
            this.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogList))
        }
        startActivity(intent)

        // 화면 전환 애니메이션 시작 후 체크 초기화 (안 보이게)
        binding.root.postDelayed({
            selectedDogList.clear()
            if (_binding != null) {
                binding.selectedDogs = selectedDogList.joinToString(", ")
            }
            homeDogListAdapter?.clearSelection()
        }, 200)
    }

    private fun showRegisterDogDialog(context: Context) {
        val dialog = SelectDialog.newInstance(title = "산책을 하기 위해 \n강아지 정보를 입력 해주세요!", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                return@OnConfirmListener
            }
            navigationManager.navigateTo(
                NavigationState.WithoutBottomNav.RegisterDog(
                    dog = null,
                    from = "home"
                )
            )
        }
        dialog.show(parentFragmentManager, "registerDog")
    }

    private fun showPermissionDialog(context: Context) {
        val dialog = SelectDialog.newInstance(title = "산책을 위해 위치 권한을 \n항상 허용으로 해주세요!", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            // 권한 창으로 이동
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.fromParts("package", context.packageName, null)
            startActivity(intent)
        }
        dialog.show(parentFragmentManager, "permission")
    }

    private fun checkLocationPermissions(): Boolean {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return !permissionHandler.checkPermissions(requireActivity(), locationPermissions)
    }

    private fun navigateToSettingAlarm() {
        navigationManager.navigateTo(NavigationState.WithoutBottomNav.SettingAlarm)
    }
}