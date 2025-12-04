package com.tulmunchi.walkingdogapp.presentation.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.IOException

/**
 * 이미지 관련 유틸리티 함수들
 */
object ImageUtils {
    /**
     * 이미지 URI가 유효한지 확인
     */
    fun isImageExists(uri: Uri, context: Context): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStream.close()
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return false
    }
}
