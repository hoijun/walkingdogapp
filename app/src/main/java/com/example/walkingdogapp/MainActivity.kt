package com.example.walkingdogapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.walkingdogapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
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
    private lateinit var mainviewmodel: userInfoViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val PERMISSIONS = arrayOf(
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(preFragment == "Mypage") {
            binding.menuBn.selectedItemId = R.id.navigation_mypage
        }

        // 위치 권한
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)

        this.onBackPressedDispatcher.addCallback(this, callback)

        if (isWalkingServiceRunning()) {
            val walkingIntent = Intent(this, WalkingActivity::class.java)
            startActivity(walkingIntent)
        }
        // 보류
        mainviewmodel = ViewModelProvider(this).get(userInfoViewModel::class.java)

        // 화면 전환
        binding.menuBn.run {
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> {
                        changeFragment(HomeFragment())
                        true
                    }

                    R.id.navigation_book -> {
                        changeFragment(BookFragment())
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
            .commit()
        binding.menuBn.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("onRequest", "onRequestPermissionsResult")
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("권한 승인", "권한 승인됨")
                    mainviewmodel.getLastLocation()
                    getUserInfo()
                } else {
                    Log.d("권한 거부", "권한 거부됨")
                    getUserInfo()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // 산책이 진행 여부
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
        val storgeRef = storage.getReference("$uid")
        lifecycleScope.launch {
            try { // 강아지 정보
                val dogDefferd = async(Dispatchers.IO) {
                    try {
                        userRef.child("dog").get().await().getValue(DogInfo::class.java)
                            ?: DogInfo()
                    } catch (e: Exception) {
                        DogInfo()
                    }
                }
                // 유저 정보
                val userDefferd = async(Dispatchers.IO) {
                    try {
                        userRef.child("user").get().await().getValue(UserInfo::class.java)
                            ?: UserInfo()
                    } catch (e: Exception) {
                        UserInfo()
                    }
                }
                // 강아지 프로필 사진
                val profileUriDeferred = async(Dispatchers.IO) {
                    try {
                        storgeRef.child("images").child("profileimg").downloadUrl.await()
                            ?: Uri.EMPTY
                    } catch (e: Exception) {
                        Uri.EMPTY
                    }
                }
                // 강아지 산책 정보
                val walkdateDeferred = suspendCoroutine { continuation ->
                    userRef.child("dog").child("walkdates").addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val dates = mutableListOf<Walkdate>()
                            if (snapshot.exists()) {
                                for (datedata in snapshot.children) {
                                    val dateinfos = datedata.key.toString().split(" ")
                                    dates.add(
                                        Walkdate(
                                            dateinfos[0], dateinfos[1], dateinfos[2],
                                            datedata.child("distance")
                                                .getValue(Float::class.java)!!,
                                            datedata.child("time").getValue(Int::class.java)!!
                                        )
                                    )
                                }
                            }
                            continuation.resume(dates)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            continuation.resume(listOf<Walkdate>())
                        }
                    })
                }

                val profileUri = profileUriDeferred.await()
                // 강아지 프로필 사진 -> drawble 형태로 변경
                val profileDrawable = suspendCoroutine { continuation ->
                    Glide.with(applicationContext).asDrawable().load(profileUri)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                continuation.resume(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                continuation.resume(null)
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                continuation.resume(null)
                            }
                        })
                }

                val dog = dogDefferd.await()
                val user = userDefferd.await()
                dog.dates = walkdateDeferred
                mainviewmodel.savedogInfo(dog)
                mainviewmodel.saveuserInfo(user)

                if (profileDrawable != null) {
                    mainviewmodel.saveImgDrawble(profileDrawable)
                }

                if (preFragment == "Mypage") {
                    changeFragment(MyPageFragment())
                } else {
                    changeFragment(HomeFragment())
                }
            } catch (e: Exception) {
                if (preFragment == "Mypage") {
                    changeFragment(MyPageFragment())
                } else {
                    changeFragment(HomeFragment())
                }
            }
        }
    }
}
