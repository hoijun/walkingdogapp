package com.example.walkingdogapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.util.TypedValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.HashMap


class Constant {
    companion object {
        const val Walking_SERVICE_ID = 175
        const val ACTION_START_Walking_SERVICE = "startWalkingService"
        const val ACTION_STOP_Walking_SERVICE = "stopWalkingService"
        const val ACTION_START_Walking_Tracking = "startWalkingTracking"
        const val ACTION_STOP_Walking_Tracking = "stopWalkingTracking"

        val item_whether = HashMap<String, Boolean>().apply {
            for (num: Int in 1..11) {
                put(String.format("%03d", num), false)
            }
        }

        fun dpTopx(dp: Float, context: Context): Int {
            val metrics = context.resources.displayMetrics;
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
        }

        fun getAge(date: String): Int {
            val currentDate = Calendar.getInstance()

            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val birthDate = dateFormat.parse(date)
            val calBirthDate = Calendar.getInstance().apply { time = birthDate }

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
    }
}