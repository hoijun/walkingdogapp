package com.example.walkingdogapp.utils.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import com.example.walkingdogapp.R
import com.example.walkingdogapp.datamodel.CollectionInfo
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
                return false
            }
            return false // 이미지가 존재하지 않음
        }

        fun setCollectionMap(): HashMap<String, CollectionInfo> {
            return hashMapOf(
                "001" to CollectionInfo(
                    "001",
                    "밥알 곰",
                    "나 이제 잘래..zz",
                    R.drawable.collection_001
                ),
                "002" to CollectionInfo(
                    "002",
                    "밥알 고양이",
                    "츄르 내놔랑 냥!",
                    R.drawable.collection_002
                ),
                "003" to CollectionInfo(
                    "003",
                    "밥알 원숭이",
                    "우끼끼! 나랑 놀자!",
                    R.drawable.collection_003
                ),
                "004" to CollectionInfo(
                    "004",
                    "밥알 펭귄",
                    "나도 날고 싶다!",
                    R.drawable.collection_004
                ),
                "005" to CollectionInfo(
                    "005",
                    "밥알 쿼카",
                    "나 만지면 벌금!",
                    R.drawable.collection_005
                ),
                "006" to CollectionInfo(
                    "006",
                    "밥알 토끼",
                    "나 달로 돌아갈래~",
                    R.drawable.collection_006
                ),
                "007" to CollectionInfo(
                    "007",
                    "노트북 하는 강아지",
                    "과제 힘들어..",
                    R.drawable.collection_007
                ),
                "008" to CollectionInfo(
                    "008",
                    "웃고있는 강아지",
                    "헤헤..",
                    R.drawable.collection_008
                ),
                "009" to CollectionInfo(
                    "009",
                    "양치하는 강아지",
                    "치카치카",
                    R.drawable.collection_009
                ),
                "010" to CollectionInfo(
                    "010",
                    "신난 코알라",
                    "시인나안다아",
                    R.drawable.collection_010
                ),
                "011" to CollectionInfo(
                    "011",
                    "신난 고양이",
                    "냥냥냥",
                    R.drawable.collection_011
                ),
                "012" to CollectionInfo(
                    "012",
                    "힘든 곰돌이",
                    "힘들어...",
                    R.drawable.collection_012
                ),
                "013" to CollectionInfo(
                    "013",
                    "하얀 강아지",
                    "멍멍!",
                    R.drawable.collection_013
                ),
                "014" to CollectionInfo(
                    "014",
                    "책 읽는 강아지",
                    "음....",
                    R.drawable.collection_014
                ),
                "015" to CollectionInfo(
                    "015",
                    "치킨 먹는 강아지",
                    "헤헤.. 맛있당",
                    R.drawable.collection_015
                ),
                "016" to CollectionInfo(
                    "016",
                    "귀여운 다람쥐",
                    "반갑습니다람쥐",
                    R.drawable.collection_016
                ),
                "017" to CollectionInfo(
                    "017",
                    "책 읽는 돼지",
                    "흡.. 휴",
                    R.drawable.collection_017
                ),
                "018" to CollectionInfo(
                    "018",
                    "행복한 곰돌이",
                    "치킨 맛있당",
                    R.drawable.collection_018
                ),
                "019" to CollectionInfo(
                    "019",
                    "일보는 강아지",
                    "저리가..",
                    R.drawable.collection_019
                ),
                "020" to CollectionInfo(
                    "020",
                    "귀여운 곰",
                    "데헷!",
                    R.drawable.collection_020
                ),
                "021" to CollectionInfo(
                    "021",
                    "핸드폰 하는 악어",
                    "뒹굴 뒹굴",
                    R.drawable.collection_021
                ),
                "022" to CollectionInfo(
                    "022",
                    "하트 강아지",
                    "이거 받아",
                    R.drawable.collection_022
                ),
                "023" to CollectionInfo(
                    "023",
                    "버블티 강아지",
                    "헤헤.. 시원해",
                    R.drawable.collection_023
                ),
                "024" to CollectionInfo(
                    "024",
                    "튜브 토끼",
                    "신난당!",
                    R.drawable.collection_024
                )
            )
        }

    }
}