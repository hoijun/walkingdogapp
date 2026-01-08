package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Walk Record (Firebase)
 */
@Keep
data class WalkRecordDto(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    @SerializedName("distance")
    val distance: Float = 0f,
    @SerializedName("time")
    val time: Int = 0,
    @SerializedName("calories")
    val calories: Float = 0f,
    @SerializedName("poop_coords")
    val poopCoordinates: List<CoordinateDto> = emptyList(),
    @SerializedName("memo_coords")
    val memoCoordinates: Map<String, CoordinateDto> = emptyMap(),
    @SerializedName("coords")
    val walkCoordinates: List<CoordinateDto> = emptyList(),
    @SerializedName("collections")
    val collections: List<String> = emptyList()
)
