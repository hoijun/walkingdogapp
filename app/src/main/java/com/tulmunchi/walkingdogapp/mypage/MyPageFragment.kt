package com.tulmunchi.walkingdogapp.mypage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentMyPageBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.gallery.GalleryFragment
import com.tulmunchi.walkingdogapp.registerinfo.RegisterUserActivity
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private var mainActivity: MainActivity? = null

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { storagePermission ->
            when (storagePermission) {
                true -> {
                    binding.countImg = getAlbumImageCount()
                }

                false -> return@registerForActivityResult
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

        MainActivity.preFragment = "Mypage"  // 다른 액티비티로 이동 할 때 마이페이지에서 이동을 표시

        context?.let { ctx ->
            if (!networkChecker.isNetworkAvailable()) {
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("인터넷을 연결해주세요!")
                builder.setPositiveButton("네", null)
                builder.setCancelable(false)
                builder.show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater,container, false)

        binding.apply {
            viewModel = mainViewModel
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
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable()) {
                        return@setOnClickListener
                    }
                }
                mainActivity?.changeFragment(SettingFragment())
            }

            val dogsList = mainViewModel.dogs.value ?: listOf()
            val myPageDogListAdapter = MyPageDogListAdapter(dogsList, mainViewModel.isSuccessGetData(), networkChecker)
            myPageDogListAdapter.onItemClickListener = MyPageDogListAdapter.OnItemClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                        return@OnItemClickListener
                    }
                }

                val dogInfoFragment = DogInfoFragment().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("doginfo", it)
                    bundle.putString("before", "mypage")
                    arguments = bundle
                }
                mainActivity?.changeFragment(dogInfoFragment)
            }

            mypageDogsViewPager.adapter = myPageDogListAdapter
            TabLayoutMediator(mypageDogsIndicator, mypageDogsViewPager) { _, _ -> }.attach()

            managedoginfoBtn.setOnClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable()) {
                        return@setOnClickListener
                    }
                }

                val manageDogsFragment = ManageDogsFragment()
                mainActivity?.changeFragment(manageDogsFragment)
            }

            modifyuserinfoBtn.setOnClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }

                    val registerUserIntent = Intent(ctx, RegisterUserActivity::class.java)
                    registerUserIntent.putExtra("userinfo", mainViewModel.user.value)
                    startActivity(registerUserIntent)
                }
            }

            managepicturesBtn.setOnClickListener {
                context?.let { ctx ->
                    if (checkPermission(storagePermission)) {
                        mainActivity?.changeFragment(GalleryFragment())
                    } else {
                        Toast.makeText(
                            ctx,
                            "갤러리 이용을 위해 권한을 모두 허용 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            menuWalkinfo.setOnClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }
                }
                mainActivity?.changeFragment(WalkInfoFragment())
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.VISIBLE)
        context?.let {
            if (checkPermission(storagePermission)) {
                binding.countImg = getAlbumImageCount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun checkPermission(permissions: Array<String>): Boolean {
        return if (!permissionHandler.checkPermissions(requireActivity(), permissions)) {
            requestStoragePermission.launch(permissions[0])
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