package com.example.walkingdogapp.mypage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.album.GalleryFragment
import com.example.walkingdogapp.databinding.FragmentMyPageBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.registerinfo.RegisterUserActivity
import com.example.walkingdogapp.userinfo.DogInfo
import com.example.walkingdogapp.userinfo.UserInfo
import com.example.walkingdogapp.userinfo.Walkdate
import com.example.walkingdogapp.userinfo.totalWalkInfo
import com.example.walkingdogapp.userinfo.userInfoViewModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var userdogInfo: DogInfo
    private lateinit var userInfo: UserInfo
    private lateinit var totalwalkInfo: totalWalkInfo
    private lateinit var walkdates: List<Walkdate>
    private lateinit var mainactivity: MainActivity

    private val storegePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                    binding.numpictures.text = "${getAlbumImageCount()}개"
                }

                false -> return@registerForActivityResult
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
        MainActivity.preFragment = "Mypage"  // 다른 액티비티로 이동 할 때 마이페이지에서 이동을 표시
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater,container, false)
        userdogInfo = myViewModel.doginfo.value ?: DogInfo()
        userInfo = myViewModel.userinfo.value ?: UserInfo()
        totalwalkInfo = myViewModel.totalwalkinfo.value ?: totalWalkInfo()
        walkdates = myViewModel.walkDates.value ?: listOf<Walkdate>()

        binding.apply {
            btnSetting.setOnClickListener {
                mainactivity.changeFragment(SettingFragment())
            }

            menuDogInfo.setOnClickListener {
                if(userdogInfo.name == "") {
                    Toast.makeText(requireContext(), "먼저 강아지 정보를 설정해주세요!", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                mainactivity.changeFragment(DogInfoFragment())
            }

            modifydoginfo.setOnClickListener {
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                registerDogIntent.putExtra("doginfo", userdogInfo)
                startActivity(registerDogIntent)
            }

            modifyuserinfo.setOnClickListener {
                val registerUserIntent = Intent(requireContext(), RegisterUserActivity::class.java)
                registerUserIntent.putExtra("userinfo", userInfo)
                startActivity(registerUserIntent)
            }

            managepictures.setOnClickListener {
                mainactivity.changeFragment(GalleryFragment())
            }

            menuWalkinfo.setOnClickListener {
                mainactivity.changeFragment(WalkInfoFragment())
            }

            if(userInfo.name != "") {
                menuUsername.text = "${userInfo.name} 님"
            }

            if(userdogInfo.name != ""){
                menuDogname.text = userdogInfo.name
                menuDogfeature.text = "${Constant.getAge(userdogInfo.birth)}살/ ${userdogInfo.weight}kg / ${userdogInfo.breed}"
                if (myViewModel.imgdrawble.value != null) {
                    menuDogimg.setImageDrawable(myViewModel.imgdrawble.value)
                }
            }

            menuDistance.text = getString(R.string.totaldistance, totalwalkInfo.totaldistance / 1000.0)

            walkDistance.text = getString(R.string.totaldistance, totalwalkInfo.totaldistance / 1000.0)
            walkTime.text =  "${(totalwalkInfo.totaltime / 60)}분"
            walkCount.text = "${walkdates.size}회"
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storegePermission)) {
            binding.numpictures.text = "${getAlbumImageCount()}개"
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
}