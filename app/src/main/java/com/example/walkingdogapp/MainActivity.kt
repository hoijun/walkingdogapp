package com.example.walkingdogapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.album.AlbumMapFragment
import com.example.walkingdogapp.collection.CollectionFragment
import com.example.walkingdogapp.databinding.ActivityMainBinding
import com.example.walkingdogapp.mainhome.HomeFragment
import com.example.walkingdogapp.mypage.ManageDogsFragment
import com.example.walkingdogapp.mypage.MyPageFragment
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkLatLng
import com.example.walkingdogapp.walking.SaveWalkDate
import com.example.walkingdogapp.walking.WalkingActivity
import com.example.walkingdogapp.walking.WalkingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var mainviewmodel: UserInfoViewModel
    private val locationPermissionRequestCode = 1000
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private var backPressedTime : Long = 0

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()

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
        var dogNameList = mutableListOf<String>()
        var dogUriList = HashMap<String, Uri>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(preFragment == "Mypage") {
            binding.menuBn.selectedItemId = R.id.navigation_mypage
        } // 다른 액티비티로 마이페이지에서 변경 했었을 경우, 다시 되돌아 올 때 바텀바의 표시를 마이페이지로 변경

        // 위치 권한
        ActivityCompat.requestPermissions(this, permissions, locationPermissionRequestCode)

        this.onBackPressedDispatcher.addCallback(this, callback)

        if (isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            startActivity(walkingIntent)
        }


        mainviewmodel = ViewModelProvider(this).get(UserInfoViewModel::class.java)

        // 화면 전환
        binding.menuBn.run {
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
        }
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.screenFl.id, fragment)
            .commitAllowingStateLoss()
        binding.menuBn.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mainviewmodel.getLastLocation()
                    getUserInfo()
                } else {
                    getUserInfo()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // 산책 진행 여부
    private fun isWalkingServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (myService in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (WalkingService::class.java.name == myService.service.className) {
                if (myService.foreground) {
                    return true
                }
            }
            return false
        }
        return false
    }

    private fun getUserInfo() {
        val uid = auth.currentUser?.uid
        val userRef = db.getReference("Users").child("$uid")
        val storageRef = storage.getReference("$uid").child("images")
        lifecycleScope.launch {
            try { // 강아지 정보
                val dogsDeferred = suspendCoroutine { continuation ->
                    userRef.child("dog").addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val dogsList = mutableListOf<DogInfo>()
                            val dogNames = mutableListOf<String>()
                            if (snapshot.exists()) {
                                for (dogInfo in snapshot.children) {
                                    dogsList.add(
                                        DogInfo(
                                            dogInfo.child("name").getValue(String::class.java)!!,
                                            dogInfo.child("breed").getValue(String::class.java)!!,
                                            dogInfo.child("gender").getValue(String::class.java)!!,
                                            dogInfo.child("birth").getValue(String::class.java)!!,
                                            dogInfo.child("neutering")
                                                .getValue(String::class.java)!!,
                                            dogInfo.child("vaccination")
                                                .getValue(String::class.java)!!,
                                            dogInfo.child("weight").getValue(Int::class.java)!!,
                                            dogInfo.child("feature").getValue(String::class.java)!!,
                                            dogInfo.child("creationTime").getValue(Long::class.java)!!,
                                            dogInfo.child("walkInfo").getValue(WalkInfo::class.java)!!
                                        )
                                    )
                                    dogNames.add(dogInfo.child("name").getValue(String::class.java)!!)
                                }
                            }
                            dogNameList = dogNames
                            continuation.resume(dogsList.sortedBy { it.creationTime })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            continuation.resume(listOf<DogInfo>())
                        }
                    })
                }

                // 유저 정보
                val userDeferred = async(Dispatchers.IO) {
                    try {
                        userRef.child("user").get().await().getValue(UserInfo::class.java)
                            ?: UserInfo()
                    } catch (e: Exception) {
                        UserInfo()
                    }
                }

                val totalWalkDeferred = async(Dispatchers.IO) {
                    try {
                        userRef.child("totalWalkInfo").get().await().getValue(WalkInfo::class.java)
                            ?: WalkInfo()
                    } catch (e: Exception) {
                        WalkInfo()
                    }
                }

                // 강아지 산책 정보
                val walkDateDeferred = suspendCoroutine { continuation ->
                    val dogsWalkRecord = HashMap<String, MutableList<WalkRecord>>()
                    for(dog in dogNameList) {
                        userRef.child("dog").child(dog).child("walkdates").addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val walkRecords = mutableListOf<WalkRecord>()
                                if (snapshot.exists()) {
                                    for (dateData in snapshot.children) {
                                        val walkDay = dateData.key.toString().split(" ")
                                        walkRecords.add(
                                            WalkRecord(
                                                walkDay[0], walkDay[1], walkDay[2],
                                                dateData.child("distance")
                                                    .getValue(Float::class.java)!!,
                                                dateData.child("time").getValue(Int::class.java)!!,
                                                dateData.child("coords").getValue<List<WalkLatLng>>() ?: listOf<WalkLatLng>(),
                                                dateData.child("collections").getValue<List<String>>() ?: listOf<String>()
                                            )
                                        )
                                    }
                                    dogsWalkRecord[dog] = walkRecords
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                dogsWalkRecord[dog] = mutableListOf()
                            }
                        })
                    }

                    continuation.resume(dogsWalkRecord)
                }

                val collectionDeferred = async(Dispatchers.IO) {
                    try {
                        userRef.child("collection").get().await().getValue<HashMap<String, Boolean>>() ?: Constant.item_whether
                    } catch (e: Exception) {
                        Constant.item_whether
                    }
                }

                // 강아지 프로필 사진
                val profileUriDeferred = suspendCoroutine { continuation ->
                    try {
                        val dogImgs = HashMap<String, Uri>()
                        var downloadCount = 0
                        var imgCount: Int
                        storageRef.listAll().addOnSuccessListener { listResult ->
                            imgCount = listResult.items.size
                            if (imgCount == 0) {
                                continuation.resume(HashMap<String, Uri>())
                            }
                            listResult.items.forEach { item ->
                                item.downloadUrl.addOnSuccessListener { uri ->
                                    downloadCount++
                                    dogImgs[item.name] = uri

                                    if(imgCount == downloadCount) {
                                        continuation.resume(dogImgs)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resume(HashMap<String, Uri>())
                    }
                }

                val user = userDeferred.await()
                val totalWalk = totalWalkDeferred.await()
                val collection = collectionDeferred.await()
                mainviewmodel.savedogsInfo(dogsDeferred)
                mainviewmodel.saveuserInfo(user)
                mainviewmodel.savetotalwalkInfo(totalWalk)
                mainviewmodel.savewalkdates(walkDateDeferred)
                mainviewmodel.savecollectionInfo(collection)
                mainviewmodel.savedogsImg(profileUriDeferred)
                dogUriList = profileUriDeferred


                binding.waitImage.visibility = View.GONE

                when (preFragment) {
                    "Mypage" -> {
                        changeFragment(MyPageFragment())
                    }
                    "Home" -> {
                        changeFragment(HomeFragment())
                    }
                    else -> {
                        changeFragment(ManageDogsFragment())
                    }
                }
            } catch (e: Exception) {
                when (preFragment) {
                    "Mypage" -> {
                        changeFragment(MyPageFragment())
                    }
                    "Home" -> {
                        changeFragment(HomeFragment())
                    }
                    else -> {
                        changeFragment(ManageDogsFragment())
                    }
                }
            }
        }
    }
}
