package com.tulmunchi.walkingdogapp.walking

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.BindingAdapter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.tulmunchi.walkingdogapp.LoadingDialogFragment
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.ActivityWalkingBinding
import com.tulmunchi.walkingdogapp.datamodel.CollectionInfo
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.tulmunchi.walkingdogapp.viewmodel.WalkingViewModel
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class WalkingActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityWalkingBinding
    private lateinit var mynavermap: NaverMap
    private val walkingViewModel: WalkingViewModel by viewModels()
    private var randomMarkerJob: Job? = null

    private var coordList = mutableListOf<LatLng>()
    private var trackingMarker = Marker()
    private var trackingPath = PathOverlay()
    private lateinit var trackingCamera: CameraUpdate
    private var trackingCameraMode = true // 지도의 화면이 자동으로 사용자에 위치에 따라 움직이는 지

    private var collectionImgViews = hashMapOf<String, ImageView>()

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
    private var collectionsMap = hashMapOf<String, CollectionInfo>()
    private lateinit var wService: WalkingService

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WalkingService.LocalBinder
            wService = binder.getService()
            binding.walkingService = wService
            setObserve()
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

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
        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)
        walkingViewModel.getLastLocation()

        selectedDogs = intent.getStringArrayListExtra("selectedDogs") ?: arrayListOf()

        // 백그라운드 위치 서비스 시작
        try {
            startWalkingService()
        } catch (e: Exception) {
            Toast.makeText(this, "오류로 인해 산책이 종료 됩니다.", Toast.LENGTH_SHORT).show()
            goHome()
        }

        collectionsMap = Utils.setCollectionMap()
        setCollectionImageView(collectionsMap)

        val mapFragment: MapFragment =
            supportFragmentManager.findFragmentById(R.id.Map) as MapFragment?
                ?: MapFragment.newInstance().also {
                    supportFragmentManager.beginTransaction().add(R.id.Map, it).commit()
                }

        mapFragment.getMapAsync(this)

        // 버튼 이벤트 설정
        binding.apply {
            lifecycleOwner = this@WalkingActivity
            isTrackingCameraMode = trackingCameraMode

            btnSave.setOnClickListener {
                selectStopWalk()
            }

            btnStart.setOnClickListener {
                if (!WalkingService.isWalkingServiceRunning()) {
                    return@setOnClickListener
                }

                if (wService.isTracking.value == true) {
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

            currentCollections.setOnClickListener {
                val currentCollections = arrayListOf<CollectionInfo>()
                wService.getCollectionItems.toMutableSet().toList().forEach {
                    currentCollections.add(collectionsMap[it]?: CollectionInfo())
                }
                val collectionListDialog = CurrentCollectionsDialog().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("currentCollections", currentCollections)
                    }
                }

                collectionListDialog.show(supportFragmentManager, "collectionList")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)
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

        walkingViewModel.currentCoord.observe(this) {
            val firstCamera = CameraUpdate.scrollAndZoomTo(
                LatLng(it.latitude, it.longitude),
                16.0
            )
            mynavermap.moveCamera(firstCamera)
            trackingMarker.position = LatLng(it.latitude, it.longitude)
            trackingMarker.map = mynavermap
        } // 현재 위치로 맵 이동

        Intent(this, WalkingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun setObserve() {
        // 위치 업데이트 이벤트
        wService.coordList.observe(this) {
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
                for (marker in wService.animalMarkers) { // 마커 특정 거리 이상일 경우 제거
                    if (marker.position.distanceTo(coordList.last()) > 350) {
                        marker.map = null
                        markersToRemove.add(marker)
                    }
                }
                wService.animalMarkers.removeAll(markersToRemove)

                if (coordList.size > 1) {
                    trackingPath.map = null
                    trackingPath.coords = coordList // 이동 경로 그림
                    trackingPath.map = mynavermap
                }
            }
        }

        if (wService.animalMarkers.isEmpty() && wService.getCollectionItems.isEmpty()) {
            randomMarker()
        }

        if (wService.animalMarkers.isNotEmpty() || wService.getCollectionItems.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(5000)
                for (animalMarker in wService.animalMarkers) {
                    if (collectionImgViews[animalMarker.tag.toString()]!!.parent != null) {
                        ((collectionImgViews[animalMarker.tag.toString()]!!.parent) as ViewGroup).removeView(
                            collectionImgViews[animalMarker.tag.toString()]
                        )
                    }

                    animalMarker.adapter =
                        object : InfoWindow.DefaultViewAdapter(this@WalkingActivity) {
                            override fun getContentView(p0: InfoWindow): View {
                                return collectionImgViews[animalMarker.tag.toString()]!!
                            }
                        }

                    animalMarker.setOnClickListener {
                        if (coordList.last().distanceTo(animalMarker.position) < 30) {
                            val tag = animalMarker.tag.toString()
                            wService.getCollectionItems.add(tag)
                            animalMarker.map = null
                            wService.animalMarkers.remove(animalMarker)

                            val getCollectionDialog = GetCollectionDialog().apply {
                                arguments = Bundle().apply {
                                    putParcelable(
                                        "getCollection",
                                        collectionsMap[tag] ?: CollectionInfo()
                                    )
                                }
                            }
                            getCollectionDialog.show(
                                supportFragmentManager,
                                "getCollection"
                            )
                        }
                        true
                    }
                    animalMarker.map = mynavermap
                }
                delay(285000)
                randomMarker()
            }
        }
    }


    private fun setCollectionImageView(collectionsMap: HashMap<String, CollectionInfo>) {
        collectionsMap.forEach { (key, value) ->
            val imgView = ImageView(this)
            imgView.layoutParams = ViewGroup.LayoutParams(200, 200)
            Glide.with(this).load(value.collectionResId).override(200, 200).into(imgView)
            collectionImgViews[key] = imgView
        }
    }

    private fun randomMarker() { // 마커 랜덤 표시
        randomMarkerJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(10000) // 10초 후에
            while (isActive && WalkingService.isWalkingServiceRunning()) {
                repeat(2) {
                    if (coordList.isNotEmpty()) {
                        if (wService.animalMarkers.size < 6) { // 마커의 갯수 상한선
                            val randomCoord = getRandomCoord(coordList.last(), 300)
                            val randomNumber = kotlin.random.Random.nextInt(1, 24)
                            val randomKey = String.format("%03d", randomNumber)
                            val animalMarker = InfoWindow()
                            if (collectionImgViews[randomKey]!!.parent != null) {
                                ((collectionImgViews[randomKey]!!.parent) as ViewGroup).removeView(
                                    collectionImgViews[randomKey]
                                )
                            }

                            animalMarker.adapter =
                                object : InfoWindow.DefaultViewAdapter(this@WalkingActivity) {
                                    override fun getContentView(p0: InfoWindow): View {
                                        return collectionImgViews[randomKey]!!
                                    }
                                }

                            animalMarker.tag = randomKey
                            animalMarker.setOnClickListener {
                                if (coordList.last().distanceTo(animalMarker.position) < 30) {
                                    val tag = animalMarker.tag.toString()
                                    wService.getCollectionItems.add(tag)
                                    animalMarker.map = null
                                    wService.animalMarkers.remove(animalMarker)

                                    val getCollectionDialog = GetCollectionDialog().apply {
                                        arguments = Bundle().apply {
                                            putParcelable(
                                                "getCollection",
                                                collectionsMap[tag] ?: CollectionInfo()
                                            )
                                        }
                                    }
                                    getCollectionDialog.show(
                                        supportFragmentManager,
                                        "getCollection"
                                    )
                                }
                                true
                            }

                            animalMarker.position = randomCoord
                            animalMarker.map = mynavermap
                            wService.animalMarkers.add(animalMarker)
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

    private fun startWalkingService() {
        if (!WalkingService.isWalkingServiceRunning()) {
            Log.d("WalkingService", "start")
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogs))
            serviceIntent.action = Utils.ACTION_START_Walking_SERVICE
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun stopWalkingService() {
        if (WalkingService.isWalkingServiceRunning()) {
            Log.d("WalkingService", "stop")
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Utils.ACTION_STOP_Walking_SERVICE
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun startTracking() {
        if (WalkingService.isWalkingServiceRunning()) {
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Utils.ACTION_START_Walking_Tracking
            startService(serviceIntent)
        }
    }

    private fun stopTracking() {
        if (WalkingService.isWalkingServiceRunning()) {
            val serviceIntent = Intent(this, WalkingService::class.java)
            serviceIntent.action = Utils.ACTION_STOP_Walking_Tracking
            startService(serviceIntent)
        }
    }

    private fun goHome() {
        val backIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("isImgChanged", false)
        }

        unbindService(connection)
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
                    if (wService.walkDistance.value!! < 300 && wService.walkTime.value!! < 300) {
                        Toast.makeText(this, "거리 또는 시간이 너무 부족해요!", Toast.LENGTH_SHORT).show()
                        stopWalkingService()
                        goHome()
                    } else {
                        startTime = wService.startTime
                        setSaveScreen()
                        saveWalkInfo(
                            wService.walkDistance.value ?: 0f,
                            wService.walkTime.value!!,
                            wService.coordList.value!!,
                            wService.walkingDogs.value ?: arrayListOf(),
                            wService.getCollectionItems.toMutableSet().toList()
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
            val error = walkingViewModel.saveWalkInfo(
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
        val coors = wService.coordList.value!!
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
        val timeStamp: String = setFileName()
        val imageFileName = "munchi_$timeStamp.jpeg"
        return File(storageDir, imageFileName)
        .apply {
            Log.i("syTest", "Created File AbsolutePath : $absolutePath")
            currentPhotoPath = absolutePath
        }
    }

    private fun setFileName(): String {
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