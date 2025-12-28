package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Coordinate (Firebase)
 */
@Keep
data class CoordinateDto(
    @SerializedName("latitude")
    val latitude: Double = 0.0,
    @SerializedName("longitude")
    val longitude: Double = 0.0
)
