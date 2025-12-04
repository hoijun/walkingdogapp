package com.tulmunchi.walkingdogapp.presentation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 날짜/시간 관련 유틸리티 함수들
 */
object DateUtils {
    /**
     * 생년월일로부터 나이 계산
     * @param date 형식: "yyyy/MM/dd"
     */
    fun getAge(date: String): Int {
        val currentDate = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val birthDate = dateFormat.parse(date)
        val calBirthDate = Calendar.getInstance().apply { time = birthDate ?: Date() }

        var age = currentDate.get(Calendar.YEAR) - calBirthDate.get(Calendar.YEAR)
        if (currentDate.get(Calendar.DAY_OF_YEAR) < calBirthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    /**
     * Long 타입 시간을 지정된 포맷의 문자열로 변환
     */
    fun convertLongToTime(format: SimpleDateFormat, time: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time * 1000
        return format.format(calendar.time)
    }
}
