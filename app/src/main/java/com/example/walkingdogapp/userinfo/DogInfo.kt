package com.example.walkingdogapp.userinfo

class DogInfo {
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
data class saveWalkdate(val distance: Float = 0.0f, val time: Int = 0, val coords: List<WalkLatLng> = listOf<WalkLatLng>())
data class WalkLatLng(val latititude: Double = 0.0, val longtitude: Double = 0.0)