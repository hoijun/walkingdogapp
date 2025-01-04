package com.tulmunchi.walkingdogapp.datamodel

import java.io.Serializable

data class TotalWalkInfo(
    var distance: Float = 0.0f,
    var time: Int = 0
) : Serializable