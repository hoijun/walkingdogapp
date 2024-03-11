package com.example.walkingdogapp.userinfo

import java.io.Serializable

class DogInfo : Serializable {
    var name = ""
    var breed = ""
    var gender = ""
    var birth = ""
    var neutering = ""
    var vaccination = ""
    var weight = 0
    var feature = ""
}
data class Walkdate(val day: String = "", val startTime: String = "", val endTime: String = "", val distance: Float = 0.0f, val time: Int = 0, val coords: List<WalkLatLng> = listOf<WalkLatLng>())
// 산책 정보를 담는 클래스
data class saveWalkdate(val distance: Float = 0.0f, val time: Int = 0, val coords: List<WalkLatLng> = listOf<WalkLatLng>()) // 산책 한 후 저장 할 때 쓰는 클래스
data class WalkLatLng(val latititude: Double = 0.0, val longtitude: Double = 0.0)