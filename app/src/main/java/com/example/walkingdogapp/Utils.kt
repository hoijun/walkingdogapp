package com.example.walkingdogapp

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.HashMap


class Utils {
    companion object {
        const val Walking_SERVICE_ID = 175
        const val ACTION_START_Walking_SERVICE = "startWalkingService"
        const val ACTION_STOP_Walking_SERVICE = "stopWalkingService"
        const val ACTION_START_Walking_Tracking = "startWalkingTracking"
        const val ACTION_STOP_Walking_Tracking = "stopWalkingTracking"

        val item_whether = HashMap<String, Boolean>().apply {
            for (num: Int in 1..24) {
                put(String.format("%03d", num), false)
            }
        }

        fun dpToPx(dp: Float, context: Context): Int {
            val metrics = context.resources.displayMetrics;
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
        }

        fun getAge(date: String): Int {
            val currentDate = Calendar.getInstance()

            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val birthDate = dateFormat.parse(date)
            val calBirthDate = Calendar.getInstance().apply { time = birthDate?: Date() }

            var age = currentDate.get(Calendar.YEAR) - calBirthDate.get(Calendar.YEAR)
            if (currentDate.get(Calendar.DAY_OF_YEAR) < calBirthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return age
        }

        fun convertLongToTime(format: SimpleDateFormat, time: Long): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time * 1000;
            return format.format(calendar.time)
        }

        fun isImageExists(uri: Uri , context: Context): Boolean {
            val contentResolver: ContentResolver = context.contentResolver
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    inputStream.close()
                    return true // 이미지가 존재함
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return false // 이미지가 존재하지 않음
        }
    }
}