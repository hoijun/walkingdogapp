package com.tulmunchi.walkingdogapp.presentation.core

import android.content.Context
import android.util.TypedValue

/**
 * UI 관련 유틸리티 함수들
 */
object UiUtils {
    /**
     * DP를 PX로 변환
     */
    fun dpToPx(dp: Float, context: Context): Int {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }
}
