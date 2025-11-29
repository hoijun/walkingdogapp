package com.tulmunchi.walkingdogapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 네트워크 연결 상태를 확인하는 인터페이스
 */
interface NetworkChecker {
    /**
     * 네트워크 연결 가능 여부를 확인
     * @return 네트워크 연결 가능하면 true, 아니면 false
     */
    fun isNetworkAvailable(): Boolean
}

/**
 * NetworkChecker 구현체
 * 기존 NetworkManager의 로직을 인터페이스 패턴으로 리팩토링
 */
class NetworkCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkChecker {

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
