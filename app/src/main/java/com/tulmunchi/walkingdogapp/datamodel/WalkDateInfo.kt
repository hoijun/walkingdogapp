package com.tulmunchi.walkingdogapp.datamodel

import android.os.Parcelable
import java.io.Serializable

@kotlinx.parcelize.Parcelize
data class WalkDateInfo(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf(),
    val collections: List<String> = listOf()
) : Serializable, Parcelable