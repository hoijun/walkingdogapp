package com.example.walkingdogapp

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMapSdk
import com.navercorp.nid.NaverIdLoginSDK
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.Kakao_API_KEY)
        NaverIdLoginSDK.initialize(this, BuildConfig.Naver_ClientId, BuildConfig.Naver_ClientSecret, "WalkDog")
    }
}