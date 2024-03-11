package com.example.walkingdogapp.walking

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.userinfo.WalkLatLng
import com.example.walkingdogapp.databinding.ActivityWalkingBinding
import com.example.walkingdogapp.userinfo.saveWalkdate
import com.example.walkingdogapp.userinfo.totalWalkInfo
import com.example.walkingdogapp.userinfo.userInfoViewModel
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
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin


class WalkingActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityWalkingBinding
    private lateinit var mynavermap: NaverMap
    private lateinit var walkViewModel: userInfoViewModel
    private var db = FirebaseDatabase.getInstance()

    private val auth = FirebaseAuth.getInstance()

    private var coordList = mutableListOf<LatLng>()
    private var isTracking = false
    private var trackingMarker = Marker()
    private var trackingPath = PathOverlay()
    private lateinit var trackingCamera: CameraUpdate
    private var trackingCameraMode = true // 지도의 화면이 자동으로 사용자에 위치에 따라 움직이는 지

    private var getCollectionItems = mutableListOf<String>()
    private var collectionImgs = listOf(
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
        R.drawable.collection_011
    )

    private var colletion_imgsBitmap = mutableListOf<Bitmap>()

    private var animalMarkers = mutableListOf<Marker>()

    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)
    private val storegePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    private val cameraCode = 98
    private val storageCode = 99

    private lateinit var uri : Uri
    private lateinit var currentPhotoPath: String

    private var startTime = ""
    private var endTime = ""

    // 뒤로 가기
    private val BackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectStopWalk()
        }
    }

    private val getResultCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            galleryAddPic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalkingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 보류
        walkViewModel = ViewModelProvider(this).get(userInfoViewModel::class.java)
        walkViewModel.getLastLocation()

        // 백그라운드 위치 서비스 시작
        startWalkingService()

        imgtoBitmap()

        val mapFragment: MapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment, it).commit()
        }

        mapFragment.getMapAsync(this)

        this.onBackPressedDispatcher.addCallback(this, BackPressCallback)

        // 트래킹 모드 여부 확인 및 여부에 따른 버튼과 텍스트 변경
        WalkingService.isTracking.observe(this) {
            this.isTracking = it
            binding.apply {
                if (isTracking) {
                    textIsTracking.text = "산책중지"
                    btnStart.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    textIsTracking.text = "산책시작"
                    btnStart.setImageResource(android.R.drawable.ic_media_play)
                }
            }
            Log.d("current coord", this.isTracking.toString())
        }

        // 버튼 이벤트 설정
        binding.apply {
            imgPencil.setOnClickListener {
                selectStopWalk()
            }

            btnStart.setOnClickListener {
                if (isWalkingServiceRunning()) {
                    if (isTracking) {
                        stopTracking()
                    } else {
                        startTracking()
                    }
                }
            }

            btnCamera.setOnClickListener {
                startCamera()
            }

            trackingCameraModeOnBtn.setOnClickListener {
                if (!trackingCameraMode) {
                    trackingCameraMode = true
                    if (coordList.isNotEmpty()) {
                        trackingCamera =
                            CameraUpdate.scrollAndZoomTo(coordList.last(), 16.0)
                                .animate(CameraAnimation.Easing)  // 현재 위치로 카메라 이동
                        mynavermap.moveCamera(trackingCamera)
                        trackingCameraModeOnBtn.setImageResource(com.nhn.android.oauth.R.drawable.naver_icon)
                    }
                    return@setOnClickListener
                }

                // 현재 위치 따라가기 였을 경우
                trackingCameraMode = false
                trackingCameraModeOnBtn.setImageResource(R.drawable.waitimage)
            }
        }
    }

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false
        mynavermap.uiSettings.isTiltGesturesEnabled = false

        trackingMarker.icon = OverlayImage.fromResource(R.mipmap.ic_launcher_round)
        trackingMarker.width = 100
        trackingMarker.height = 100
        trackingMarker.anchor = PointF(0.5f, 0.5f)

        trackingPath.width = 15
        trackingPath.color = Color.YELLOW

        randomMarker()

        walkViewModel.currentCoord.observe(this) {
            val firstCamera = CameraUpdate.scrollAndZoomTo(LatLng(it.latitude, it.longitude),
                16.0)
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

                if(trackingCameraMode) {
                    trackingCamera =
                        CameraUpdate.scrollAndZoomTo(coordList.last(), 16.0)
                            .animate(CameraAnimation.Easing)  // 현재 위치로 카메라 이동
                    mynavermap.moveCamera(trackingCamera)
                }

                val markersToRemove = mutableListOf<Marker>()
                for (marker in animalMarkers) { // 마커 특정 거리 이상일 경우 제거
                    if (marker.position.distanceTo(coordList.last()) > 400) {
                        Log.d("coor", "clear")
                        marker.map = null
                        markersToRemove.add(marker)
                    }
                }
                animalMarkers.removeAll(markersToRemove)
                
                if (coordList.size > 1) {
                    trackingPath.map = null
                    trackingPath.coords = coordList // 이동 경로 그림
                    trackingPath.map = mynavermap
                    binding.InfoDistance.text = getString(
                        R.string.distance,
                        WalkingService.walkDistance / 1000.0)
                }
            }
        }

        WalkingService.walkTime.observe(this) {
            val minutes = it / 60
            val seconds = it % 60
            binding.InfoTime.text = getString(R.string.time, minutes, seconds)
        }
    }

    private fun imgtoBitmap() {
        lifecycleScope.launch(Dispatchers.Main) {
            val option = BitmapFactory.Options()
            option.inSampleSize = 6
            for (img in collectionImgs) {
                colletion_imgsBitmap.add(BitmapFactory.decodeResource(resources, img, option))
            }
        }
    }

    private fun randomMarker() { // 마커 랜덤 표시
        lifecycleScope.launch(Dispatchers.Main) {
            delay(10000) // 10초 후에
            while (isWalkingServiceRunning()) {
                repeat(2) {
                    if (coordList.isNotEmpty()) {
                        if (animalMarkers.size < 6) { // 마커의 갯수 상한선
                            val randomCoord = getRandomCoord(coordList.last(), 300)
                            val randomNumber = kotlin.random.Random.nextInt(1, 12)
                            val animalMarker = Marker()
                            animalMarker.icon =
                                OverlayImage.fromBitmap(colletion_imgsBitmap[randomNumber - 1])
                            animalMarker.width = 100
                            animalMarker.height = 100
                            animalMarker.anchor = PointF(0.5f, 0.5f)
                            animalMarker.tag = String.format("%03d", randomNumber)
                            animalMarker.setOnClickListener {
                                if (coordList.last().distanceTo(animalMarker.position) < 20) {
                                    getCollectionItems.add(animalMarker.tag.toString())
                                    animalMarker.map = null
                                    animalMarkers.remove(animalMarker)
                                }
                                true
                            }
                            animalMarker.position = randomCoord
                            animalMarker.map = mynavermap
                            animalMarkers.add(animalMarker)
                        }
                    }
                }
                delay(300000) // 5분 마다
            }
        }
    }

    private fun getRandomCoord(currentCoord: LatLng ,rad : Int) : LatLng { // 랜덤 좌표
        val random = Random()
        val minDistance = 50
        val randomDistance = (minDistance + (random.nextDouble() * (rad - minDistance))) / 111000
        val randomBearing = random.nextDouble() * 360

        val randomLat = currentCoord.latitude + randomDistance * sin(Math.toRadians(randomBearing))
        val randomLong = currentCoord.longitude + randomDistance * cos(Math.toRadians(randomBearing))

        return LatLng(randomLat, randomLong)
    }

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

    private fun startWalkingService() {
        if (!isWalkingServiceRunning()) {
            Log.d("WalkingService", "start")
            val serviceIntent = Intent(this, WalkingService::class.java)
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("산책 그만 할까요?")

        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (WalkingService.walkDistance < 300 || WalkingService.walkTime.value!! < 300) {
                        Toast.makeText(this, "거리 또는 시간이 너무 부족해요!",Toast.LENGTH_SHORT).show()
                        stopWalkingService()
                        goHome()
                    }
                    else {
                        startTime = WalkingService.startTime
                        setSaveScreen()
                        saveWalkInfo(
                            WalkingService.walkDistance, WalkingService.walkTime.value!!,
                            WalkingService.coordList.value!!)
                    }
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니오", null)
        builder.show()
    }

    // 거리 및 시간 저장, 날짜: 시간 별로 산책 기록 저장
    private fun saveWalkInfo(distance: Float, time: Int, coords: List<LatLng>) {
        val uid = auth.currentUser?.uid
        val userRef = db.getReference("Users").child("$uid")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                val walkDateinfo =
                    LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + startTime + " " + endTime

                val total = snapshot.child("totalWalk").getValue(totalWalkInfo::class.java)

                var error = false

                lifecycleScope.launch {
                    val totalwalkJob = async(Dispatchers.IO) {
                        try {
                            if (total != null) {
                                userRef.child("totalWalk").setValue(totalWalkInfo(total.totaldistance + distance,total.totaltime + time)).await()
                            } else {
                                userRef.child("totalWalk").setValue(totalWalkInfo(distance, time))
                                    .await()
                            }
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val saveWalkdateJob = async(Dispatchers.IO) {
                        try {
                            val savecoords = mutableListOf<WalkLatLng>()
                            for (coord in coords) {
                                savecoords.add(WalkLatLng(coord.latitude, coord.longitude))
                            }
                            userRef.child("walkdates").child(walkDateinfo).setValue(saveWalkdate(distance, time, savecoords)).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val collectionInfojob = async(Dispatchers.IO) {
                        try {
                            val update = mutableMapOf<String, Any>()
                            for (item in getCollectionItems) {
                                update[item] = true
                            }

                            userRef.child("collection").updateChildren(update).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    totalwalkJob.await()
                    saveWalkdateJob.await()
                    collectionInfojob.await()

                    if (error) {
                        Toast.makeText(this@WalkingActivity, "산책 기록 저장 실패", Toast.LENGTH_SHORT)
                            .show()
                    }

                    stopWalkingService()
                    goHome()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WalkingActivity, "산책 기록 저장 실패", Toast.LENGTH_SHORT).show()
                stopWalkingService()
                goHome()
            }
        })
    }

    private fun setSaveScreen() {
        binding.walkingscreen.visibility = View.INVISIBLE
        binding.waitImage.visibility = View.VISIBLE

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
        if(checkPermission(cameraPermission, cameraCode) && checkPermission(storegePermission, storageCode)) {
            takePhoto(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }
    }

    private fun takePhoto(intent: Intent) {
        val parentDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absoluteFile
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
                "walkingdogapp.provider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            getResultCamera.launch(intent)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(storageDir: File?): File {
        // Create an image file name
        val timeStamp: String = RandomFileName()
        val imageFileName = "munchi_$timeStamp.jpeg"
        return File(storageDir, imageFileName)
        .apply {
            Log.i("syTest", "Created File AbsolutePath : $absolutePath")
            currentPhotoPath = absolutePath
        }
    }

    fun RandomFileName(): String {
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
                        startCamera()
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
                    startCamera()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}