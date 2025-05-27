package com.tulmunchi.walkingdogapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.navercorp.nid.NaverIdLoginSDK
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.Kakao_API_KEY)
        NaverIdLoginSDK.initialize(this, BuildConfig.Naver_ClientId, BuildConfig.Naver_ClientSecret, "WalkDog")
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(BuildConfig.NaverMap_API_KEY)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}