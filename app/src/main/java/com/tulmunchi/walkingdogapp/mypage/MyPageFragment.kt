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
import com.tulmunchi.walkingdogapp.gallery.GalleryFragment
import com.tulmunchi.walkingdogapp.databinding.FragmentMyPageBinding
import com.tulmunchi.walkingdogapp.datamodel.DogInfo
import com.tulmunchi.walkingdogapp.datamodel.TotalWalkInfo
import com.tulmunchi.walkingdogapp.datamodel.WalkDateInfo
import com.tulmunchi.walkingdogapp.registerinfo.RegisterUserActivity
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private var totalWalkInfo = TotalWalkInfo()
    private var walkdates = mutableListOf<WalkDateInfo>()
    private var mainActivity: MainActivity? = null

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
            if (!NetworkManager.checkNetworkState(ctx)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("인터넷을 연결해주세요!")
                builder.setPositiveButton("네", null)
                builder.setCancelable(false)
                builder.show()
            }
        }

        val walkRecordList = mainViewModel.walkDates.value?: hashMapOf()
        for(dog in MainActivity.dogNameList) {
            for(date in walkRecordList[dog] ?: listOf()) {
                walkdates.add(date)
            }
        }

        walkdates = walkdates.toMutableSet().toMutableList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater,container, false)
        totalWalkInfo = mainViewModel.totalWalkInfo.value ?: TotalWalkInfo()

        binding.apply {
            viewmodel = mainViewModel
            lifecycleOwner = viewLifecycleOwner

            refresh.apply {
                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        mainViewModel.observeUser()
                    }
                }
                mainViewModel.successGetData.observe(viewLifecycleOwner) {
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
                    if (!NetworkManager.checkNetworkState(ctx)) {
                        return@setOnClickListener
                    }
                }
                mainActivity?.changeFragment(SettingFragment())
            }

            val dogsList = mainViewModel.dogsInfo.value ?: listOf()
            val myPageDogListAdapter = MyPageDogListAdapter(dogsList, mainViewModel.isSuccessGetData())
            myPageDogListAdapter.onItemClickListener = MyPageDogListAdapter.OnItemClickListener {
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
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
                    if (!NetworkManager.checkNetworkState(ctx)) {
                        return@setOnClickListener
                    }
                }

                val manageDogsFragment = ManageDogsFragment()
                mainActivity?.changeFragment(manageDogsFragment)
            }

            modifyuserinfoBtn.setOnClickListener {
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }

                    val registerUserIntent = Intent(ctx, RegisterUserActivity::class.java)
                    registerUserIntent.putExtra("userinfo", mainViewModel.userInfo.value)
                    startActivity(registerUserIntent)
                }
            }

            managepicturesBtn.setOnClickListener {
                context?.let { ctx ->
                    if (checkPermission(storagePermission, ctx)) {
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
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
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
        context?.let { ctx ->
            if (checkPermission(storagePermission, ctx)) {
                binding.countImg = getAlbumImageCount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun checkPermission(permissions: Array<out String>, context: Context): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermission.launch(permission)
                return false
            }
        }
        return true
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
        fun setWalkCountText(textView: TextView, walkDates: HashMap<String, MutableList<WalkDateInfo>>?, dogs: List<DogInfo>?) {
            if (walkDates != null && dogs != null) {
                val totalWalk = mutableListOf<WalkDateInfo>()
                for (dog in dogs) {
                    totalWalk.addAll(walkDates.get(dog.name) ?: mutableListOf())
                }

                val counts = totalWalk.groupingBy { it }.eachCount()

                textView.text = counts.size.toString() + "회"
            }
        }
    }
}