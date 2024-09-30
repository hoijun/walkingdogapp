package com.example.walkingdogapp.walking

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.walkingdogapp.Utils
import com.example.walkingdogapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.InfoWindow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import kotlin.concurrent.timer

class WalkingService : Service() {
    private lateinit var locationRequest: LocationRequest
    private lateinit var builder : NotificationCompat.Builder
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var walkTimer: Timer? = Timer()
    private var miscount = 0
    private var totalTime = 0

    companion object {
        val coordList = MutableLiveData<MutableList<LatLng>>()
        val isTracking = MutableLiveData<Boolean>()
        val walkTime = MutableLiveData<Int>()
        var getCollectionItems = mutableListOf<String>()
        var walkingDogs = MutableLiveData<ArrayList<String>>()
        var animalMarkers = mutableListOf<InfoWindow>()
        var walkDistance = MutableLiveData<Float>()
        var startTime = ""
    }

    private fun postInitialValue() {
        isTracking.postValue(false)
        walkTime.postValue(0)
        coordList.postValue(mutableListOf())
        totalTime = 0
        miscount = 0
        walkingDogs.postValue(arrayListOf())
        getCollectionItems = mutableListOf()
        animalMarkers = mutableListOf<InfoWindow>()
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
                        // 위치 업데이트 간의 거리가 클 경우 업데이트 안함 및 이 상황이 1번일 경우는 통과
                        if (coordList.value!!.isNotEmpty() && coordList.value!!.last().distanceTo(pos) > 5f && miscount < 1) {
                            miscount++
                            return
                        }

                        coordList.value!!.apply {
                            add(pos)
                            coordList.postValue(this)
                            miscount = 0
                        }

                        if (coordList.value!!.size > 1) { // 거리 증가
                            walkDistance.postValue(
                                walkDistance.value?.plus(
                                    coordList.value!!.last()
                                        .distanceTo(coordList.value!![coordList.value!!.size - 2])
                                        .toFloat()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValue()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            val action = intent.action
            if (action != null) {
                when(action) {
                    Utils.ACTION_START_Walking_SERVICE -> {
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
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
                return
            }
        }

        val channelId = "WalkingDogApp_Channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val resultIntent = Intent(applicationContext, WalkingActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)
        builder = NotificationCompat.Builder(applicationContext, channelId)
        builder.setSmallIcon(R.drawable.appicon)
        builder.setContentTitle("털뭉치")
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        builder.setContentText("산책중 이에요.")
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(false)
        builder.setOngoing(true)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannel(
                channelId,
                "Walking Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "This channel is used by walking service"
            notificationManager.createNotificationChannel(notificationChannel)
        }


        locationRequest = LocationRequest.Builder(2500)
            .setMinUpdateIntervalMillis(2500)
            .setMaxUpdateDelayMillis(2500)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        isTracking.postValue(true)

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
}