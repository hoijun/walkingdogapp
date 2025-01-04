package com.tulmunchi.walkingdogapp.datamodel

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class WalkDateInfoInSave(
    @SerializedName("distance")
    val distance: Float = 0.0f,
    @SerializedName("time")
    val time: Int = 0,
    @SerializedName("coords")
    val coords: List<WalkLatLng> = listOf(),
    @SerializedName("collections")
    val collections: List<String> = listOf()
)