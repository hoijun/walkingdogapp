package com.tulmunchi.walkingdogapp.datamodel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@kotlinx.parcelize.Parcelize
data class WalkDateInfo(
    @SerializedName("day")
    val day: String = "",
    @SerializedName("startTime")
    val startTime: String = "",
    @SerializedName("endTime")
    val endTime: String = "",
    @SerializedName("distance")
    val distance: Float = 0.0f,
    @SerializedName("time")
    val time: Int = 0,
    @SerializedName("coords")
    val coords: List<WalkLatLng> = listOf(),
    @SerializedName("collections")
    val collections: List<String> = listOf()
) : Serializable, Parcelable