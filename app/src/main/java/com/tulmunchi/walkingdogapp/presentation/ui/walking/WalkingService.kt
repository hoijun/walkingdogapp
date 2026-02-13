package com.tulmunchi.walkingdogapp.presentation.ui.walking

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.math.atan2

@AndroidEntryPoint
class WalkingService : Service(), SensorEventListener {
    companion object {
        private var isWalkingServiceRunning = false
        fun isWalkingServiceRunning(): Boolean {
            return isWalkingServiceRunning
        }
    }

    private var binder = LocalBinder()
    private lateinit var locationRequest: LocationRequest
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var walkTimer: Timer? = Timer()
    private var totalTime = 0
    private var angleThreshold = 30

    // 센서 관련
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    @Inject
    lateinit var permissionHandler: PermissionHandler

    val walkCoordList = MutableLiveData<MutableList<LatLng>>()
    val isTracking = MutableLiveData(true)
    val walkTime = MutableLiveData<Int>()
    var walkDistance = MutableLiveData<Float>()
    var calories = MutableLiveData<Float>()
    var speed = MutableLiveData<Float>()
    var bearing = MutableLiveData(0f)
    var poopMarkers = mutableListOf<LatLng>()
    var memoMarkers = mutableListOf<LatLng>()
    var memos = hashMapOf<LatLng, String>()
    var getCollectionItems = mutableListOf<String>()
    var walkingDogs = ArrayList<String>()
    var walkingDogsWeights = ArrayList<Int>()
    var animalMarkers = mutableListOf<InfoWindow>()
    var startTime = ""

    inner class LocalBinder: Binder() {
        fun getService(): WalkingService = this@WalkingService
    }

    override fun onCreate() {
        super.onCreate()
        // 센서 매니저 초기화
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    private fun postInitialValue() {
        walkCoordList.postValue(mutableListOf())
        isTracking.postValue(true)
        walkTime.postValue(0)
        walkDistance.postValue(0f)
        calories.postValue(0f)
        speed.postValue(0f)
        poopMarkers = mutableListOf()
        memoMarkers = mutableListOf()
        memos = hashMapOf()
        getCollectionItems = mutableListOf()
        walkingDogs = arrayListOf()
        walkingDogsWeights = arrayListOf()
        animalMarkers = mutableListOf()
        startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        totalTime = 0
    }

    // 위치 업데이트
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (isTracking.value == true) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        val pos = LatLng(location.latitude, location.longitude)
                        if (walkCoordList.value == null) {
                            return
                        }

                        speed.postValue(location.speed)

                        if (walkCoordList.value!!.isNotEmpty() && walkCoordList.value!!.last().distanceTo(pos) < 3f) {
                            return
                        }

                        walkCoordList.value!!.apply {
                            add(pos)
                            walkCoordList.postValue(this)
                        }

                        val previousDistance = walkDistance.value ?: 0f

                        if (walkCoordList.value!!.size > 1) { // 거리 증가
                            walkDistance.postValue(
                                walkDistance.value?.plus(
                                    walkCoordList.value!!.last()
                                        .distanceTo(walkCoordList.value!![walkCoordList.value!!.size - 2])
                                        .toFloat()
                                )
                            )
                        }

                        if (walkCoordList.value!!.size > 2) {
                            val lastThree = walkCoordList.value!!.takeLast(3)
                            if (needToFlat(lastThree[0], lastThree[1], lastThree[2])) {
                                walkCoordList.value!!.removeAt(walkCoordList.value!!.size - 2)
                                walkCoordList.postValue(walkCoordList.value!!)
                            }
                        }

                        if (hasPassedKm(previousDistance.toInt(), walkDistance.value?.toInt() ?: 0)) {
                            angleThreshold -= 2
                            if (angleThreshold < 0) {
                                angleThreshold = 20
                            }
                        }

                        calories.postValue(calculateCalories(walkDistance.value ?: 0f))
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            val action = intent.action
            if (action != null) {
                when(action) {
                    WalkingConstants.ACTION_START_WALKING_SERVICE -> {
                        postInitialValue()
                        isWalkingServiceRunning = true
                        walkingDogs = intent.getStringArrayListExtra("selectedDogs")?: arrayListOf()
                        walkingDogsWeights = intent.getIntegerArrayListExtra("selectedDogsWeights")?: arrayListOf()
                        startLocationService()
                    }

                    WalkingConstants.ACTION_STOP_WALKING_SERVICE -> {
                        stopLocationService()
                    }

                    WalkingConstants.ACTION_START_WALKING_TRACKING -> {
                        startTracking()
                    }

                    WalkingConstants.ACTION_STOP_WALKING_TRACKING -> {
                        stopTracking()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationService() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (!permissionHandler.checkPermissions(this, locationPermissions)) {
            return
        }

        val channelId = "WalkingDogApp_Channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val resultIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("털뭉치")
            .setContentText("산책중 이에요.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannel(
                channelId,
                "Walking Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "This channel is used by walking service"
            notificationManager.createNotificationChannel(notificationChannel)
        }

        locationRequest = LocationRequest.Builder(5000)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 센서 리스너 등록 (NORMAL = ~5Hz, 배터리 절약)
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(WalkingConstants.WALKING_SERVICE_ID, builder.build())
        } else {
            startForeground(
                WalkingConstants.WALKING_SERVICE_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        }
        startTimer()
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        isTracking.postValue(true)
        startTimer()
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ) // 위치 업데이트 재개
    }

    private fun stopTracking() {
        isTracking.postValue(false)
        stopTimer()
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback) // 위치 업데이트 중단
    }

    private fun stopLocationService() {
        isTracking.postValue(false)
        isWalkingServiceRunning = false
        stopTimer()
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        // 센서 리스너 해제
        sensorManager.unregisterListener(this)
        postInitialValue()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        walkTimer = timer(initialDelay = 500, period = 1000) {
            totalTime++
            walkTime.postValue(totalTime)
        }
    }

    private fun stopTimer() {
        walkTimer?.cancel()
    }

    fun needToFlat(point1: LatLng, point2: LatLng, point3: LatLng): Boolean {
        var needToFlat = false
        val angle1 = calculateAngle(point1, point2)
        val angle2 = calculateAngle(point2, point3)

        if (abs(angle1 - angle2) <= angleThreshold) {
            needToFlat = true
        }

        return needToFlat
    }

    private fun calculateAngle(point1: LatLng, point2: LatLng): Float {
        val dx = point2.longitude - point1.longitude
        val dy = point2.latitude - point1.latitude

        var angle = Math.toDegrees(atan2(dx, dy)).toFloat()

        if (angle < 0) {
            angle += 360f
        }

        return angle
    }

    private fun calculateCalories(totalDistance: Float): Float {
        val sumOfWeights = walkingDogsWeights.sum()
        return (sumOfWeights * totalDistance / 1000 * 0.8).toFloat()
    }


    fun hasPassedKm(previousDistance: Int, currentDistance: Int): Boolean {
        val previousThousand = previousDistance / 1000
        val currentThousand = currentDistance / 1000
        return currentThousand > previousThousand
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val normalizedBearing = if (azimuth < 0) azimuth + 360f else azimuth

            val smoothedBearing = smoothBearing(bearing.value ?: 0f, normalizedBearing, 0.2f)
            bearing.postValue(smoothedBearing)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    private fun smoothBearing(oldBearing: Float, newBearing: Float, alpha: Float): Float {
        var diff = newBearing - oldBearing
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        return (oldBearing + diff * alpha + 360f) % 360f
    }
}