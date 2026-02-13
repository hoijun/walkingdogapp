package com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentMyPageBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MyPageViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val myPageViewModel: MyPageViewModel by activityViewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var navigationManager: NavigationManager

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                myPageViewModel.loadImageCount()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!networkChecker.isNetworkAvailable()) {
            val dialog = SelectDialog.newInstance(title = "인터넷을 연결해주세요!")
            dialog.isCancelable = false
            dialog.show(parentFragmentManager, "networkCheck")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater,container, false)

        val currentCountCollection = mainViewModel.collections.value?.count { it.value } ?: 0

        binding.apply {
            viewModel = mainViewModel
            countCollection = currentCountCollection
            lifecycleOwner = viewLifecycleOwner

            refresh.apply {
                this.setOnRefreshListener {
                    mainViewModel.loadUserData()
                }
                mainViewModel.dataLoadSuccess.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            mypageDogsViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    refresh.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE)
                }
            })

            btnSetting.setOnClickListener {
                if (!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                navigationManager.navigateTo(NavigationState.WithoutBottomNav.Setting)
            }

            val dogsList = mainViewModel.dogs.value ?: listOf()
            val dogImages = mainViewModel.dogImages.value ?: emptyMap()
            val myPageDogListAdapter = MyPageDogListAdapter(dogsList, mainViewModel.isSuccessGetData(), networkChecker, dogImages)
            myPageDogListAdapter.onItemClickListener = MyPageDogListAdapter.OnItemClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                        return@OnItemClickListener
                    }
                }

                navigationManager.navigateTo(
                    NavigationState.WithoutBottomNav.DogInfo(
                        dog = it,
                        before = "myPage"
                    )
                )
            }

            myPageDogListAdapter.onAddDogClickListener =
                MyPageDogListAdapter.OnAddDogClickListener {
                    navigationManager.navigateTo(
                        NavigationState.WithoutBottomNav.RegisterDog(
                            dog = null,
                            from = "myPage"
                        )
                    )
                }

            mypageDogsViewPager.adapter = myPageDogListAdapter
            TabLayoutMediator(mypageDogsIndicator, mypageDogsViewPager) { _, _ -> }.attach()

            btnManageDog.setOnClickListener {
                if (!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                navigationManager.navigateTo(NavigationState.WithoutBottomNav.ManageDogs)
            }

            btnManageUser.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }
                navigationManager.navigateTo(NavigationState.WithoutBottomNav.RegisterUser(from = "myPage"))
            }

            btnManageGallery.setOnClickListener {
                context?.let { ctx ->
                    if (checkPermission(storagePermission)) {
                        navigationManager.navigateTo(NavigationState.WithoutBottomNav.Gallery())
                    } else {
                        Toast.makeText(
                            ctx,
                            "갤러리 이용을 위해 권한을 모두 허용 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            menuWalkInfoLayout.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }
                navigationManager.navigateTo(
                    NavigationState.WithoutBottomNav.WalkInfo(
                        selectDateRecord = null,
                        selectDog = null
                    )
                )
            }
        }

        // ViewModel 데이터 관찰
        myPageViewModel.imageCount.observe(viewLifecycleOwner) { count ->
            binding.countImg = count
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            if (checkPermission(storagePermission)) {
                myPageViewModel.loadImageCount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermission(permissions: Array<String>): Boolean {
        return if (!permissionHandler.checkPermissions(requireActivity(), permissions)) {
            requestStoragePermission.launch(permissions)
            false
        } else {
            true
        }
    }

    object MyPageBindingAdapter {
        @BindingAdapter("walkDates", "dogs")
        @JvmStatic
        fun setWalkCountText(textView: TextView, walkDates: Map<String, List<WalkRecord>>?, dogs: List<Dog>?) {
            if (walkDates != null && dogs != null) {
                val totalWalk = mutableListOf<WalkRecord>()
                for (dog in dogs) {
                    totalWalk.addAll(walkDates[dog.name] ?: listOf())
                }

                val counts = totalWalk.groupingBy { it }.eachCount()

                textView.text = counts.size.toString() + "회"
            }
        }
    }
}