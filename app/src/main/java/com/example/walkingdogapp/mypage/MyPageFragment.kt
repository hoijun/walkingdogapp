package com.example.walkingdogapp.mypage

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.album.GalleryFragment
import com.example.walkingdogapp.databinding.FragmentMyPageBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.registerinfo.RegisterUserActivity
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.prolificinteractive.materialcalendarview.CalendarDay


class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val userDataViewModel: UserInfoViewModel by activityViewModels()
    private lateinit var totalWalkInfo: WalkInfo
    private var walkdates = mutableListOf<WalkRecord>()
    private lateinit var mainactivity: MainActivity

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
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
        MainActivity.preFragment = "Mypage"  // 다른 액티비티로 이동 할 때 마이페이지에서 이동을 표시

        val walkRecordList = userDataViewModel.walkDates.value?: hashMapOf()
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
        totalWalkInfo = userDataViewModel.totalWalkInfo.value ?: WalkInfo()

        binding.apply {
            viewmodel = userDataViewModel
            lifecycleOwner = requireActivity()

            btnSetting.setOnClickListener {
                mainactivity.changeFragment(SettingFragment())
            }

            val dogsList = userDataViewModel.dogsInfo.value ?: listOf()
            val myPageDogListAdapter = MyPageDogListAdapter(dogsList)
            myPageDogListAdapter.onItemClickListener = MyPageDogListAdapter.OnItemClickListener {
                val dogInfoFragment = DogInfoFragment().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("doginfo", it)
                    bundle.putString("before", "mypage")
                    arguments = bundle
                }
                mainactivity.changeFragment(dogInfoFragment)
            }

            mypageDogsViewPager.adapter = myPageDogListAdapter
            TabLayoutMediator(mypageDogsIndicator, mypageDogsViewPager) { _, _ -> }.attach()

            managedoginfoBtn.setOnClickListener {
                val manageDogsFragment = ManageDogsFragment()
                mainactivity.changeFragment(manageDogsFragment)
            }

            modifyuserinfoBtn.setOnClickListener {
                val registerUserIntent = Intent(requireContext(), RegisterUserActivity::class.java)
                registerUserIntent.putExtra("userinfo", userDataViewModel.userInfo.value)
                startActivity(registerUserIntent)
            }

            managepicturesBtn.setOnClickListener {
                if (checkPermission(storagePermission)) {
                    mainactivity.changeFragment(GalleryFragment())
                } else {
                    Toast.makeText(requireContext(), "갤러리 이용을 위해 권한을 모두 허용 해주세요!", Toast.LENGTH_SHORT).show()
                }
            }

            menuWalkinfo.setOnClickListener {
                mainactivity.changeFragment(WalkInfoFragment())
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storagePermission)) {
            binding.countImg = getAlbumImageCount()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermission(permissions: Array<out String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
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
        var count = 0
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("털뭉치", "%munchi_%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val cursor = requireActivity().contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use { cursor ->
            while (cursor.moveToNext()) {
                count++
            }
        }
        return count
    }

    object MyPageBindingAdapter {
        @BindingAdapter("walkDates", "dogs")
        @JvmStatic
        fun setWalkCountText(textView: TextView, walkDates: HashMap<String, MutableList<WalkRecord>>, dogs: List<DogInfo>) {
            val totalWalk = mutableListOf<WalkRecord>()
            for(dog in dogs) {
                totalWalk.addAll(walkDates.get(dog.name) ?: mutableListOf())
            }

            val counts = totalWalk.groupingBy { it }.eachCount()

            textView.text = counts.size.toString() + "회"
        }
    }
}