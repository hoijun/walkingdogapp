package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Walk Record (Firebase)
 */
@Keep
data class WalkRecordDto(
    @SerializedName("day")
    val day: String = "",
    @SerializedName("startTime")
    val startTime: String = "",
    @SerializedName("endTime")
    val endTime: String = "",
    @SerializedName("distance")
    val distance: Float = 0f,
    @SerializedName("time")
    val time: Int = 0,
    @SerializedName("coords")
    val coords: List<CoordinateDto> = emptyList(),
    @SerializedName("collections")
    val collections: List<String> = emptyList()
)
