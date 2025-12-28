package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Walk Statistics (Firebase)
 */
@Keep
data class WalkStatsDto(
    @SerializedName("distance")
    val distance: Float = 0f,
    @SerializedName("time")
    val time: Int = 0
)
