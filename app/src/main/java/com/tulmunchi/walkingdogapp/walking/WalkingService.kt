package com.tulmunchi.walkingdogapp.walking

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.InfoWindow
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.utils.Utils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.math.atan2

class WalkingService : Service() {
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

    val coordList = MutableLiveData<MutableList<LatLng>>()
    val isTracking = MutableLiveData<Boolean>()
    val walkTime = MutableLiveData<Int>()
    var getCollectionItems = mutableListOf<String>()
    var walkingDogs = MutableLiveData<ArrayList<String>>()
    var animalMarkers = mutableListOf<InfoWindow>()
    var walkDistance = MutableLiveData<Float>()
    var startTime = ""

    inner class LocalBinder: Binder() {
        fun getService(): WalkingService = this@WalkingService
    }

    private fun postInitialValue() {
        isTracking.postValue(true)
        walkTime.postValue(0)
        coordList.postValue(mutableListOf())
        totalTime = 0
        walkingDogs.postValue(arrayListOf())
        getCollectionItems = mutableListOf()
        animalMarkers = mutableListOf()
        walkDistance.postValue(0f)
        startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    // 위치 업데이트
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (isTracking.value == true) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        val pos = LatLng(location.latitude, location.longitude)
                        if (coordList.value == null) {
                            return
                        }

                        if (coordList.value!!.isNotEmpty() && coordList.value!!.last().distanceTo(pos) < 3f) {
                            return
                        }

                        coordList.value!!.apply {
                            add(pos)
                            coordList.postValue(this)
                        }

                        val previousDistance = walkDistance.value ?: 0f

                        if (coordList.value!!.size > 1) { // 거리 증가
                            walkDistance.postValue(
                                walkDistance.value?.plus(
                                    coordList.value!!.last()
                                        .distanceTo(coordList.value!![coordList.value!!.size - 2])
                                        .toFloat()
                                )
                            )
                        }

                        if (coordList.value!!.size > 2) {
                            val lastThree = coordList.value!!.takeLast(3)
                            if (needToFlat(lastThree[0], lastThree[1], lastThree[2])) {
                                coordList.value!!.removeAt(coordList.value!!.size - 2)
                                coordList.postValue(coordList.value!!)
                            }
                        }

                        if (hasPassedKm(previousDistance.toInt(), walkDistance.value?.toInt() ?: 0)) {
                            angleThreshold -= 2
                            if (angleThreshold < 0) {
                                angleThreshold = 20
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            val action = intent.action
            if (action != null) {
                when(action) {
                    Utils.ACTION_START_Walking_SERVICE -> {
                        postInitialValue()
                        isWalkingServiceRunning = true
                        walkingDogs.postValue(intent.getStringArrayListExtra("selectedDogs")?: arrayListOf())
                        startLocationService()
                    }

                    Utils.ACTION_STOP_Walking_SERVICE -> {
                        stopLocationService()
                    }

                    Utils.ACTION_START_Walking_Tracking -> {
                        startTracking()
                    }

                    Utils.ACTION_STOP_Walking_Tracking -> {
                        stopTracking()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startLocationService() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "WalkingDogApp_Channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val resultIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.appicon)
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


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(Utils.Walking_SERVICE_ID, builder.build())
        } else {
            startForeground(
                Utils.Walking_SERVICE_ID,
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
        postInitialValue()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        walkTimer = timer(initialDelay = 500, period = 1000) {
            totalTime++
            walkTime.postValue(totalTime)
            Log.d("walktime", totalTime.toString())
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

        // 각도를 0에서 360도 사이의 값으로 조정
        if (angle < 0) {
            angle += 360f
        }

        return angle
    }

    fun hasPassedKm(previousDistance: Int, currentDistance: Int): Boolean {
        val previousThousand = previousDistance / 1000
        val currentThousand = currentDistance / 1000
        return currentThousand > previousThousand
    }
}