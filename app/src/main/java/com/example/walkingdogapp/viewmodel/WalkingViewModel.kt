package com.example.walkingdogapp.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.walkingdogapp.repository.UserInfoRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class WalkingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UserInfoRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
): ViewModel() {
    private val _currentCoord = MutableLiveData<LatLng>()

    val currentCoord: LiveData<LatLng>
        get() = _currentCoord

    suspend fun saveWalkInfo(walkDogs: ArrayList<String>, startTime: String, distance: Float, time: Int, coords: List<com.naver.maps.geometry.LatLng>, collections: List<String>): Boolean {
        return repository.saveWalkInfo(walkDogs, startTime, distance, time, coords, collections)
    }

    fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    Log.d("savepoint", "위도: ${it.latitude}, 경도: ${it.longitude}")
                    updateLocateInfo(LatLng(it.latitude, it.longitude))
                }
            }
        }
    }

    private fun updateLocateInfo(input: LatLng) {
        _currentCoord.value = input
    }
}