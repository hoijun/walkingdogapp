package com.example.walkingdogapp.datamodel

import android.os.Parcelable
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
    var weight: String = "",
    var feature: String = "",
    var creationTime: Long = 0,
    var walkInfo: WalkInfo = WalkInfo()
) : Serializable

data class WalkInfo(var distance: Float = 0.0f, var time: Int = 0) : Serializable

@kotlinx.parcelize.Parcelize
data class WalkRecord(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf<WalkLatLng>(),
    val collections: List<String> = listOf<String>()
) : Serializable, Parcelable
// 산책 정보를 담는 클래스


data class WalkLatLng(val latitude: Double = 0.0, val longitude: Double = 0.0) : Serializable

@kotlinx.parcelize.Parcelize
data class CollectionInfo(
    val collectionNum: String = "000",
    val collectionName: String = "",
    val collectionText: String = "",
    val collectionResId: Int = 0
) : Serializable, Parcelable