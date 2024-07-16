package com.example.walkingdogapp.walking

import android.Manifest
import android.app.ActivityManager
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.LoadingDialogFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.ActivityWalkingBinding
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkLatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

data class SaveWalkDate (
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf<WalkLatLng>(),
    val collections: List<String>
) // 산책 한 후 저장 할 때 쓰는 클래스

class WalkingActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityWalkingBinding
    private lateinit var mynavermap: NaverMap
    private lateinit var userInfoViewModel: UserInfoViewModel

    private var coordList = mutableListOf<LatLng>()
    private var trackingMarker = Marker()
    private var trackingPath = PathOverlay()
    private lateinit var trackingCamera: CameraUpdate
    private var trackingCameraMode = true // 지도의 화면이 자동으로 사용자에 위치에 따라 움직이는 지

    private var collectionResources = listOf(
        R.drawable.collection_001,
        R.drawable.collection_002,
        R.drawable.collection_003,
        R.drawable.collection_004,
        R.drawable.collection_005,
        R.drawable.collection_006,
        R.drawable.collection_007,
        R.drawable.collection_008,
        R.drawable.collection_009,
        R.drawable.collection_010,
        R.drawable.collection_011,
        R.drawable.collection_012,
        R.drawable.collection_013,
        R.drawable.collection_014,
        R.drawable.collection_015,
        R.drawable.collection_016,
        R.drawable.collection_017,
        R.drawable.collection_018,
        R.drawable.collection_019,
        R.drawable.collection_020,
        R.drawable.collection_021,
        R.drawable.collection_022,
        R.drawable.collection_023,
        R.drawable.collection_024
    )

    private var collectionImgViews = mutableListOf<ImageView>()

    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)
    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    private val cameraCode = 98
    private val storageCode = 99

    private lateinit var uri: Uri
    private lateinit var currentPhotoPath: String

    private var startTime = ""
    private var selectedDogs = arrayListOf<String>()

    // 뒤로 가기
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectStopWalk()
        }
    }

    private val getResultCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                galleryAddPic()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalkingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 보류
        userInfoViewModel = ViewModelProvider(this).get(UserInfoViewModel::class.java)
        userInfoViewModel.getLastLocation()

        selectedDogs = intent.getStringArrayListExtra("selectedDogs") ?: arrayListOf()

        // 백그라운드 위치 서비스 시작
        try {
            startWalkingService()
        } catch (e: Exception) {
            Toast.makeText(this, "오류로 인해 산책이 종료 됩니다.", Toast.LENGTH_SHORT).show()
            goHome()
        }

        setCollectionImageView()

        val mapFragment: MapFragment =
            supportFragmentManager.findFragmentById(R.id.Map) as MapFragment?
                ?: MapFragment.newInstance().also {
                    supportFragmentManager.beginTransaction().add(R.id.Map, it).commit()
                }

        mapFragment.getMapAsync(this)

        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // 버튼 이벤트 설정
        binding.apply {
            walkingService = WalkingService
            lifecycleOwner = this@WalkingActivity
            isTrackingCameraMode = trackingCameraMode

            btnSave.setOnClickListener {
                selectStopWalk()
            }

            btnStart.setOnClickListener {
                if (!isWalkingServiceRunning()) {
                    return@setOnClickListener
                }

                if (WalkingService.isTracking.value == true) {
                    stopTracking()
                } else {
                    startTracking()
                }
            }

            btnCamera.setOnClickListener {
                startCamera()
            }

            trackingCameraModeOnBtn.setOnClickListener {
                if (!trackingCameraMode) {
                    trackingCameraMode = true
                    isTrackingCameraMode = trackingCameraMode
                    if (coordList.isNotEmpty()) {
                        trackingCamera = CameraUpdate.scrollAndZoomTo(coordList.last(), 16.0)
                            .animate(CameraAnimation.Easing)  // 현재 위치로 카메라 이동
                        mynavermap.moveCamera(trackingCamera)
                    }
                    return@setOnClickListener
                }

                // 현재 위치 따라가기 였을 경우
                trackingCameraMode = false
                isTrackingCameraMode = trackingCameraMode
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "오류가 생겨 산책이 종료 되었습니다", Toast.LENGTH_SHORT).show()
                stopWalkingService()
                goHome()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "오류가 생겨 산책이 종료 되었습니다", Toast.LENGTH_SHORT).show()
                stopWalkingService()
                goHome()
            }
        }
    }

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false
        mynavermap.uiSettings.isTiltGesturesEnabled = false

        trackingMarker.icon = OverlayImage.fromResource(R.drawable.walkicon)
        trackingMarker.width = 200
        trackingMarker.height = 200
        trackingMarker.anchor = PointF(0.5f, 0.5f)

        trackingPath.width = 15
        trackingPath.color = Color.YELLOW

        userInfoViewModel.currentCoord.observe(this) {
            val firstCamera = CameraUpdate.scrollAndZoomTo(
                LatLng(it.latitude, it.longitude),
                16.0
            )
            mynavermap.moveCamera(firstCamera)
            trackingMarker.position = LatLng(it.latitude, it.longitude)
            trackingMarker.map = mynavermap
        } // 현재 위치로 맵 이동

        // 위치 업데이트 이벤트
        WalkingService.coordList.observe(this) {
            coordList = it
            if (coordList.isNotEmpty()) {
                trackingMarker.map = null
                trackingMarker.position = coordList.last()  // 현재 위치에 아이콘 설정
                trackingMarker.map = mynavermap

                if (trackingCameraMode) {
                    trackingCamera =
                        CameraUpdate.scrollAndZoomTo(coordList.last(), 16.0)
                            .animate(CameraAnimation.Easing)  // 현재 위치로 카메라 이동
                    mynavermap.moveCamera(trackingCamera)
                }

                val markersToRemove = mutableListOf<InfoWindow>()
                for (marker in WalkingService.animalMarkers) { // 마커 특정 거리 이상일 경우 제거
                    if (marker.position.distanceTo(coordList.last()) > 400) {
                        marker.map = null
                        markersToRemove.add(marker)
                    }
                }
                WalkingService.animalMarkers.removeAll(markersToRemove)

                if (coordList.size > 1) {
                    trackingPath.map = null
                    trackingPath.coords = coordList // 이동 경로 그림
                    trackingPath.map = mynavermap
                }
            }
        }

        if (WalkingService.animalMarkers.isEmpty()) {
            randomMarker()
        }

        if (WalkingService.animalMarkers.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(5000)
                for (animalMarker in WalkingService.animalMarkers) {
                    animalMarker.adapter =
                        object : InfoWindow.DefaultViewAdapter(this@WalkingActivity) {
                            override fun getContentView(p0: InfoWindow): View {
                                return collectionImgViews[animalMarker.tag.toString().toInt()]
                            }
                        }
                    animalMarker.map = mynavermap
                }
            }
        }
    }

    private fun setCollectionImageView() {
        for (imgRes in collectionResources) {
            val imgView = ImageView(this)
            imgView.layoutParams = ViewGroup.LayoutParams(200, 200)
            Glide.with(this).load(imgRes).override(200, 200).into(imgView)
            collectionImgViews.add(imgView)
        }
    }

    private fun randomMarker() { // 마커 랜덤 표시
        lifecycleScope.launch(Dispatchers.Main) {
            delay(10000) // 10초 후에
            while (isWalkingServiceRunning()) {
                repeat(2) {
                    if (coordList.isNotEmpty()) {
                        if (WalkingService.animalMarkers.size < 6) { // 마커의 갯수 상한선
                            val randomCoord = getRandomCoord(coordList.last(), 300)
                            val randomNumber = kotlin.random.Random.nextInt(1, 12)
                            val animalMarker = InfoWindow()
                            if (collectionImgViews[randomNumber - 1].parent != null) {
                                ((collectionImgViews[randomNumber - 1].parent) as ViewGroup).removeView(
                                    collectionImgViews[randomNumber - 1]
                                )
                            }

                            animalMarker.adapter =
                                object : InfoWindow.DefaultViewAdapter(this@WalkingActivity) {
                                    override fun getContentView(p0: InfoWindow): View {
                                        return collectionImgViews[randomNumber - 1]
                                    }
                                }

                            animalMarker.tag = String.format("%03d", randomNumber)
                            animalMarker.setOnClickListener {
                                if (coordList.last().distanceTo(animalMarker.position) < 20) {
                                    WalkingService.getCollectionItems.add(animalMarker.tag.toString())
                                    animalMarker.map = null
                                    WalkingService.animalMarkers.remove(animalMarker)
                                }
                                true
                            }
                            animalMarker.position = randomCoord
                            animalMarker.map = mynavermap
                            WalkingService.animalMarkers.add(animalMarker)
                        }
                    }
                }
                delay(300000) // 5분 마다
            }
        }
    }

    private fun getRandomCoord(currentCoord: LatLng, rad: Int): LatLng { // 랜덤 좌표
        val random = Random()
        val minDistance = 50
        val randomDistance = (minDistance + (random.nextDouble() * (rad - minDistance))) / 111000
        val randomBearing = random.nextDouble() * 360

        val randomLat = currentCoord.latitude + randomDistance * sin(Math.toRadians(randomBearing))
        val randomLong =
            currentCoord.longitude + randomDistance * cos(Math.toRadians(randomBearing))

        return LatLng(randomLat, randomLong)
    }

    private fun isWalkingServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
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

    private fun startWalkingService() {
        if (!isWalkingServiceRunning()) {
            Log.d("WalkingService", "start")
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogs))
            serviceIntent.action = Constant.ACTION_START_Walking_SERVICE
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun stopWalkingService() {
        if (isWalkingServiceRunning()) {
            Log.d("WalkingService", "stop")
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Constant.ACTION_STOP_Walking_SERVICE
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun startTracking() {
        if (isWalkingServiceRunning()) {
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Constant.ACTION_START_Walking_Tracking
            startService(serviceIntent)
        }
    }

    private fun stopTracking() {
        if (isWalkingServiceRunning()) {
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Constant.ACTION_STOP_Walking_Tracking
            startService(serviceIntent)
        }
    }

    private fun goHome() {
        val backIntent = Intent(this@WalkingActivity, MainActivity::class.java)
        backIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(backIntent)
        finish()
    }

    private fun selectStopWalk() {
        if(!NetworkManager.checkNetworkState(this)) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("산책 그만 할까요?\n(5분 또는 300m 이상 산책 시 기록 가능)")

        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (WalkingService.walkDistance.value!! < 300 && WalkingService.walkTime.value!! < 300) {
                        Toast.makeText(this, "거리 또는 시간이 너무 부족해요!", Toast.LENGTH_SHORT).show()
                        stopWalkingService()
                        goHome()
                    } else {
                        startTime = WalkingService.startTime
                        setSaveScreen()
                        saveWalkInfo(
                            WalkingService.walkDistance.value ?: 0f,
                            WalkingService.walkTime.value!!,
                            WalkingService.coordList.value!!,
                            WalkingService.walkingDogs.value ?: arrayListOf(),
                            WalkingService.getCollectionItems.toMutableSet().toList()
                        )
                    }
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니오", null)
        builder.show()
    }

    // 거리 및 시간 저장, 날짜: 시간 별로 산책 기록 저장
    private fun saveWalkInfo(
        distance: Float,
        time: Int,
        coords: List<LatLng>,
        walkDogs: ArrayList<String>,
        collections: List<String>
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadingDialogFragment = LoadingDialogFragment()
            loadingDialogFragment.show(this@WalkingActivity.supportFragmentManager, "loading")
            val error = userInfoViewModel.saveWalkInfo(
                walkDogs,
                startTime,
                distance,
                time,
                coords,
                collections
            )

            withContext(Dispatchers.Main) {
                loadingDialogFragment.dismiss()
                if (error) {
                    Toast.makeText(this@WalkingActivity, "산책 기록 저장 실패", Toast.LENGTH_SHORT).show()
                }
                stopWalkingService()
                goHome()
            }
        }
    }

    private fun setSaveScreen() {
        val coors = WalkingService.coordList.value!!
        if (coors.isNotEmpty()) {
            val middleCoor = coors[coors.size / 2]
            val saveCamera = CameraUpdate.scrollAndZoomTo(
                LatLng(middleCoor.latitude, middleCoor.longitude),
                12.5
            )
            mynavermap.moveCamera(saveCamera)
        }
    }

    private fun startCamera() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("지도 앨범에 사진을 추가하려면\n카메라 설정에서 위치 태그를 켜주세요!")

        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if(checkPermission(cameraPermission, cameraCode) && checkPermission(storagePermission, storageCode)) {
                        takePhoto(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    }
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.show()
    }

    private fun takePhoto(intent: Intent) {
        val parentDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absoluteFile
        if(!parentDir.exists()) {
            parentDir.mkdir()
        }
        val subDir = File(parentDir, "털뭉치")
        if (!subDir.exists()) {
            subDir.mkdir()
        }
        val photoFile: File? = try {
            createImageFile(subDir)
        } catch (ex: IOException) {
            // Error occurred while creating the File
            ex.printStackTrace()
            null
        }

        photoFile?.also {
            uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            getResultCamera.launch(intent)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(storageDir: File?): File {
        // Create an image file name
        val timeStamp: String = randomFileName()
        val imageFileName = "munchi_$timeStamp.jpeg"
        return File(storageDir, imageFileName)
        .apply {
            Log.i("syTest", "Created File AbsolutePath : $absolutePath")
            currentPhotoPath = absolutePath
        }
    }

    private fun randomFileName(): String {
        return SimpleDateFormat(
            "yyyyMMddHHmmss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
    }


    private fun galleryAddPic() {
        MediaScannerConnection.scanFile(
            this,
            arrayOf(currentPhotoPath),
            null
        ) { path, uri ->
            Log.d("testsave", "Scanned file path: $path")
            Log.d("testsave", "Scanned file URI: $uri")
        }
    }

    private fun checkPermission(permissions : Array<out String>, code: Int) : Boolean{
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, code)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            storageCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("사진 저장을 위해 권한을 \n허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = FLAG_ACTIVITY_NEW_TASK
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            takePhoto(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    }
                }
            }
            cameraCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("촬영을 위해 권한을 \n허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = FLAG_ACTIVITY_NEW_TASK
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                } else {
                    if(checkPermission(storagePermission, storageCode)) {
                        takePhoto(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    object WalkingBindingAdapter {
        @BindingAdapter("isTracking")
        @JvmStatic
        fun setStartBtnImg(iv: ImageView, isTracking: Boolean?) {
            if(isTracking == true) {
                Glide.with(iv.context).load(R.drawable.pause).into(iv)
            } else {
                Glide.with(iv.context).load(R.drawable.play).into(iv)
            }
        }

        @BindingAdapter("dogsList")
        @JvmStatic
        fun setWalkDogsText(tv: TextView, dogsList: ArrayList<String>?) {
            if (dogsList != null) {
                tv.text = dogsList.joinToString(", ") + " 산책 중.."
            }
        }

        @BindingAdapter("isTrackingCameraMode")
        @JvmStatic
        fun setTrackingCameraModeOnBtn(iv: ImageView, isTrackingCameraMode: Boolean) {
            if (isTrackingCameraMode) {
                Glide.with(iv.context).load(R.drawable.mylocation).into(iv)
            } else {
                Glide.with(iv.context).load(R.drawable.location_disabled).into(iv)
            }
        }
    }
}