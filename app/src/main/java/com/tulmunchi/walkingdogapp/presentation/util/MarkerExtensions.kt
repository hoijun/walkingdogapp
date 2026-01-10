package com.tulmunchi.walkingdogapp.presentation.util

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage

/**
 * 마커 아이콘 설정 확장 함수
 * API 28 이상: 기존 방식 사용
 * API 27 이하: 비트맵 변환으로 벡터/WebP 호환성 문제 해결
 */
fun Marker.setIconCompat(context: Context, resId: Int, size: Int) {
    this.width = size
    this.height = size
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this.icon = OverlayImage.fromResource(resId)
        return
    }
    
    val drawable = ContextCompat.getDrawable(context, resId) ?: return
    val bitmap = createBitmap(size, size)
    Canvas(bitmap).apply {
        drawable.setBounds(0, 0, size, size)
        drawable.draw(this)
    }
    this.icon = OverlayImage.fromBitmap(bitmap)
}
