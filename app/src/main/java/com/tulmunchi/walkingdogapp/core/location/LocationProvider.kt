package com.tulmunchi.walkingdogapp.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

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
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): LatLng? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(LatLng(location.latitude, location.longitude))
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}
