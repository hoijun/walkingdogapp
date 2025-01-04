package com.tulmunchi.walkingdogapp.datamodel

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class DogInfo(
    @SerializedName("name")
    var name: String = "",
    @SerializedName("breed")
    var breed: String = "",
    @SerializedName("gender")
    var gender: String = "",
    @SerializedName("birth")
    var birth: String = "",
    @SerializedName("neutering")
    var neutering: String = "",
    @SerializedName("vaccination")
    var vaccination: String = "",
    @SerializedName("weight")
    var weight: String = "",
    @SerializedName("feature")
    var feature: String = "",
    @SerializedName("creationTime")
    var creationTime: Long = 0,
    @SerializedName("totalWalkInfo")
    var totalWalkInfo: TotalWalkInfo = TotalWalkInfo()
) : Serializable