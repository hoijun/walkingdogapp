package com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

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
                binding.countImg = getAlbumImageCount()
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
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            if (checkPermission(storagePermission)) {
                binding.countImg = getAlbumImageCount()
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

    private fun getAlbumImageCount(): Int {
        val contentResolver = activity?.contentResolver ?: return 0

        try {
            var count = 0
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection: String
            val selectionArgs: Array<String>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.IS_PENDING} = 0"
                selectionArgs = arrayOf("털뭉치", "%munchi_%")
            } else {
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                selectionArgs = arrayOf("털뭉치", "%munchi_%")
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            cursor?.use {
                while (it.moveToNext()) {
                    count++
                }
            }
            return count
        } catch (e: Exception) {
            context?.let { ctx ->
                Toast.makeText(ctx, "이미지를 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT)
                    .show()
            }
            return 0
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