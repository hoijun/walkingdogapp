package com.example.walkingdogapp.walking

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.userinfo.WalkLatLng
import com.example.walkingdogapp.databinding.ActivityWalkingBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.cos
import kotlin.math.sin


class WalkingActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityWalkingBinding
    private lateinit var mynavermap: NaverMap
    private lateinit var walkViewModel: userInfoViewModel
    private var db = FirebaseDatabase.getInstance()
    // private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var coordList = mutableListOf<LatLng>()
    private var isTracking = false
    private var trackingMarker = Marker()
    private var trackingPath = PathOverlay()
    private lateinit var trackingCamera : CameraUpdate

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

        startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

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
                    if(isTracking) {
                        stopTracking()
                    }
                    else {
                        startTracking()
                    }
                }
            }

            btnCamera.setOnClickListener {
                startCamera()
            }
        }
    }

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false

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
                trackingCamera =
                    CameraUpdate.scrollAndZoomTo(coordList.last(), 16.0).animate(CameraAnimation.Easing)  // 현재 위치로 카메라 이동
                mynavermap.moveCamera(trackingCamera)

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
                        WalkingService.totalDistance / 1000.0)
                }
            }
        }

        WalkingService.walkTime.observe(this) {
            val minutes = it / 60
            val seconds = it % 60
            binding.InfoTime.text = getString(R.string.time, minutes, seconds)
        }
    }

    private fun randomMarker() { // 마커 랜덤 표시
        lifecycleScope.launch(Dispatchers.Main) {
            delay(10000) // 10초 후에
            while (isWalkingServiceRunning()) {
                repeat(2) {
                    if (coordList.isNotEmpty()) {
                        val randomCoord = getRandomCoord(coordList.last(), 300)
                        val animalMarker = Marker()
                        animalMarker.icon = OverlayImage.fromResource(R.mipmap.ic_launcher_round)
                        animalMarker.width = 100
                        animalMarker.height = 100
                        animalMarker.anchor = PointF(0.5f, 0.5f)
                        animalMarker.setOnClickListener {
                            if (coordList.last().distanceTo(animalMarker.position) < 5) {
                                animalMarker.map = null
                                animalMarkers.remove(animalMarker)
                            }
                            true
                        }
                        if (animalMarkers.size < 6) { // 마커의 갯수 상한선
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
                    if (WalkingService.totalDistance < 500 || WalkingService.walkTime.value!! < 300) {
                        Toast.makeText(this, "거리 또는 시간이 너무 부족해요!",Toast.LENGTH_SHORT).show()
                    }
                    else {
                        setSaveScreen()
                        saveWalkInfo(
                            WalkingService.totalDistance, WalkingService.walkTime.value!!,
                            WalkingService.coordList.value!!)
                    }
                    stopWalkingService()
                    goHome()
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
        val userRef = db.getReference("Users").child("$uid").child("user")
        val dogRef = db.getReference("Users").child("$uid").child("dog")
        // val storgeRef = storage.getReference("$uid")
        // val baos = ByteArrayOutputStream()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                val walkDateinfo =
                    LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + startTime + " " + endTime
                val totalDistance =
                    snapshot.child("totaldistance").getValue(Long::class.java)?.toFloat()
                val totalTime =
                    snapshot.child("totaltime").getValue(Long::class.java)?.toInt()

                var error = false

                lifecycleScope.launch {
                    val totaldistanceJob = async(Dispatchers.IO) {
                        try {
                            if (totalDistance != null) {
                                userRef.child("totaldistance")
                                    .setValue(totalDistance + distance)
                                    .await()
                            }
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val totaltimeJob = async(Dispatchers.IO) {
                        try {
                            if (totalTime != null) {
                                Log.d("totalTime", WalkingService.walkTime.value!!.toString())
                                userRef.child("totaltime").setValue(totalTime + time).await()
                            }
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val datedistanceJob = async(Dispatchers.IO) {
                        try {
                            dogRef.child("walkdates").child(walkDateinfo)
                                .child("distance").setValue(distance).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val datetimeJob = async(Dispatchers.IO) {
                        try {
                            dogRef.child("walkdates").child(walkDateinfo)
                                .child("time").setValue(time).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val dateCoordsJob = async(Dispatchers.IO) {
                        try {
                            val savecoords = mutableListOf<WalkLatLng>()
                            for (coord in coords) {
                                savecoords.add(WalkLatLng(coord.latitude, coord.longitude))
                            }
                            dogRef.child("walkdates").child(walkDateinfo)
                                .child("coords").setValue(savecoords).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }

                    val bitmap = getMapBitmap()

                    // 산책 화면 사진으로 저장
                    /* val mapcaptureJob = async(Dispatchers.IO) {
                        try {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val data = baos.toByteArray()
                            storgeRef.child("images").child("mapCapture").child(walkDateinfo)
                                .putBytes(data).await()
                        } catch (e: Exception) {
                            error = true
                        }
                    }*/

                    totaldistanceJob.await()
                    totaltimeJob.await()
                    datedistanceJob.await()
                    datetimeJob.await()
                    // mapcaptureJob.await()
                    dateCoordsJob.await()

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

    private suspend fun getMapBitmap(): Bitmap {
        return suspendCoroutine { continuation ->
            mynavermap.takeSnapshot(false) {
                continuation.resume(it)
            }
        }
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