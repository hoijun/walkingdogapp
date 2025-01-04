package com.tulmunchi.walkingdogapp.utils.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.datamodel.CollectionInfo
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
                    "밥알 집냥이",
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
                    "밥알 핑크토끼",
                    "나 달로 돌아갈래~",
                    R.drawable.collection_006
                ),
                "007" to CollectionInfo(
                    "007",
                    "밥알 시바견",
                    "엄살 아니다! 멍!",
                    R.drawable.collection_007
                ),
                "008" to CollectionInfo(
                    "008",
                    "밥알 공룡",
                    "내가 다시 나타났다!",
                    R.drawable.collection_008
                ),
                "009" to CollectionInfo(
                    "009",
                    "밥알 루돌프",
                    "춥다...",
                    R.drawable.collection_009
                ),
                "010" to CollectionInfo(
                    "010",
                    "밥알 고슴도치",
                    "따끔따끔 하지!",
                    R.drawable.collection_010
                ),
                "011" to CollectionInfo(
                    "011",
                    "밥알 병아리",
                    "엄마 어딨어! 삐약!",
                    R.drawable.collection_011
                ),
                "012" to CollectionInfo(
                    "012",
                    "밥알 호랑이",
                    "어흥! 떡 내놔!",
                    R.drawable.collection_012
                ),
                "013" to CollectionInfo(
                    "013",
                    "밥알 돼지",
                    "밥 달라! 꿀꿀!",
                    R.drawable.collection_013
                ),
                "014" to CollectionInfo(
                    "014",
                    "밥알 비글",
                    "내 밥이다! 멍!",
                    R.drawable.collection_014
                ),
                "015" to CollectionInfo(
                    "015",
                    "밥알 코끼리",
                    "모자 멋있지. 뿌우!",
                    R.drawable.collection_015
                ),
                "016" to CollectionInfo(
                    "016",
                    "밥알 상어",
                    "크아앙! 내 공을 받아라!",
                    R.drawable.collection_016
                ),
                "017" to CollectionInfo(
                    "017",
                    "밥알 판다",
                    "대나무 맛있당...",
                    R.drawable.collection_017
                ),
                "018" to CollectionInfo(
                    "018",
                    "밥알 누룽지냥이",
                    "이 털뭉치는 뭐냥..",
                    R.drawable.collection_018
                ),
                "019" to CollectionInfo(
                    "019",
                    "밥알 하얀 토끼",
                    "깡총! 당근은 내꺼당!",
                    R.drawable.collection_019
                ),
                "020" to CollectionInfo(
                    "020",
                    "밥알 랫서판다",
                    "크앙! 무섭지!",
                    R.drawable.collection_020
                ),
                "021" to CollectionInfo(
                    "021",
                    "밥알 하마",
                    "수박을 한입에 앙!",
                    R.drawable.collection_021
                ),
                "022" to CollectionInfo(
                    "022",
                    "밥알 햄스터",
                    "입안에 넣어 놔야지!",
                    R.drawable.collection_022
                ),
                "023" to CollectionInfo(
                    "023",
                    "밥알 닭",
                    "엄마 여기 있어! 꼬끼오!",
                    R.drawable.collection_023
                ),
                "024" to CollectionInfo(
                    "024",
                    "밥알 다람쥐",
                    "도토리 더 없나...",
                    R.drawable.collection_024
                )
            )
        }

    }
}