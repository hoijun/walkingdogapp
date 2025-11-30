package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Dog (Firebase)
 */
@Keep
data class DogDto(
    @SerializedName("name")
    val name: String = "",
    @SerializedName("breed")
    val breed: String = "",
    @SerializedName("gender")
    val gender: String = "",
    @SerializedName("birth")
    val birth: String = "",
    @SerializedName("neutering")
    val neutering: String = "",
    @SerializedName("vaccination")
    val vaccination: String = "",
    @SerializedName("weight")
    val weight: String = "",
    @SerializedName("feature")
    val feature: String = "",
    @SerializedName("creationTime")
    val creationTime: Long = 0,
    @SerializedName("totalWalkInfo")
    val totalWalkInfo: WalkStatsDto = WalkStatsDto()
)
