package com.tulmunchi.walkingdogapp.presentation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.ui.walking.WalkingActivity
import com.tulmunchi.walkingdogapp.presentation.viewmodel.HomeViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.WalkValidationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
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

            // Observe selected dogs text
            homeViewModel.selectedDogsText.observe(viewLifecycleOwner) { text ->
                selectedDogs = text
            }

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
                if (!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                navigateToSettingAlarm()
            }

            val dogsList = mainViewModel.dogs.value ?: listOf()
            val dogImages = mainViewModel.dogImages.value ?: emptyMap()

            homeDogListAdapter = HomeDogListAdapter(dogsList, dogImages)

            homeDogListAdapter?.onClickDogListener = HomeDogListAdapter.OnClickDogListener { dog ->
                homeViewModel.toggleDogSelection(
                    dog.name,
                    dog.weight.toInt()
                )
            }

            homeDogListAdapter?.onAddDogClickListener = HomeDogListAdapter.OnAddDogClickListener {
                navigationManager.navigateTo(
                    NavigationState.WithoutBottomNav.RegisterDog(
                        dog = null,
                        from = "home"
                    )
                )
            }

            homeDogsViewPager.adapter = homeDogListAdapter
            TabLayoutMediator(homeDogsIndicator, homeDogsViewPager) { _, _ -> }.attach()

            btnWalk.setOnClickListener { handleWalkButtonClick(dogsList) }
        }
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.clearSelection()
        homeDogListAdapter?.clearSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeDogListAdapter = null
        _binding = null
    }

    private fun handleWalkButtonClick(dogsList: List<Dog>) {
        val ctx = context ?: return

        // Check network first
        if (!networkChecker.isNetworkAvailable()) {
            return
        }

        // Validate walk start using HomeViewModel
        val validationResult = homeViewModel.validateWalkStart(
            dogsList = dogsList,
            isDataLoaded = mainViewModel.isSuccessGetData(),
            hasLocationPermission = checkLocationPermissions()
        )

        when (validationResult) {
            is WalkValidationResult.Success -> {
                // Start walking activity
                val intent = Intent(ctx, WalkingActivity::class.java).apply {
                    putStringArrayListExtra("selectedDogs", homeViewModel.getSelectedDogNames())
                    putIntegerArrayListExtra("selectedDogsWeights", homeViewModel.getSelectedDogWeights())
                }
                startActivity(intent)

                binding.root.postDelayed({
                    binding.homeDogsViewPager.setCurrentItem(0, false)
                }, 200)
            }
            is WalkValidationResult.DataNotLoaded -> { }
            is WalkValidationResult.NoDogRegistered -> {
                showRegisterDogDialog()
            }
            is WalkValidationResult.NoDogSelected -> {
                Toast.makeText(ctx, "함께 산책할 강아지를 선택해주세요!", Toast.LENGTH_SHORT).show()
            }
            is WalkValidationResult.LocationPermissionDenied -> {
                showPermissionDialog(ctx)
            }
            is WalkValidationResult.NetworkUnavailable -> { }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionHandler.checkPermissions(requireActivity(), locationPermissions)
    }

    private fun showRegisterDogDialog() {
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

    private fun navigateToSettingAlarm() {
        navigationManager.navigateTo(NavigationState.WithoutBottomNav.SettingAlarm)
    }
}