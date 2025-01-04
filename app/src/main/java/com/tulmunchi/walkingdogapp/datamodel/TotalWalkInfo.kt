package com.tulmunchi.walkingdogapp.datamodel

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class TotalWalkInfo(
    @SerializedName("distance")
    var distance: Float = 0.0f,
    @SerializedName("time")
    var time: Int = 0
) : Serializable