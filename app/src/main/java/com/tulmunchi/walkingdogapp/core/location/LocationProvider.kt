package com.tulmunchi.walkingdogapp.core.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 위치 정보 제공 인터페이스
 */
interface LocationProvider {
    /**
     * 마지막으로 알려진 위치를 가져옴
     * @return 위치 정보 (LatLng) 또는 null
     */
    suspend fun getLastLocation(): LatLng?
}

/**
 * LocationProvider 구현체
 * Google Play Services의 FusedLocationProviderClient를 사용
 */
class LocationProviderImpl @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): LatLng? {
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }
}
