package com.example.walkingdogapp.datamodel

import java.io.Serializable

data class UserInfo(
    var email: String = "",
    var name: String = "",
    var gender: String = "",
    var birth: String = "",
) : Serializable

data class DogInfo(
    var name: String = "",
    var breed: String = "",
    var gender: String = "",
    var birth: String = "",
    var neutering: String = "",
    var vaccination: String = "",
    var weight: Int = 0,
    var feature: String = "",
    var creationTime: Long = 0,
    var walkInfo: WalkInfo = WalkInfo()
) : Serializable

data class WalkInfo(var totaldistance: Float = 0.0f, var totaltime: Int = 0) : Serializable

data class WalkDate(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf<WalkLatLng>(),
    val dogs: List<String> = listOf<String>(),
    val collections: List<String> = listOf<String>()
) : Serializable

// 산책 정보를 담는 클래스
data class SaveWalkDate (
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf<WalkLatLng>(),
    val dogs: List<String>,
    val collections: List<String>
) // 산책 한 후 저장 할 때 쓰는 클래스

data class WalkLatLng(val latititude: Double = 0.0, val longtitude: Double = 0.0) : Serializable