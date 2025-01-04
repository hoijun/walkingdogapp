package com.tulmunchi.walkingdogapp.datamodel

import java.io.Serializable

data class DogInfo(
    var name: String = "",
    var breed: String = "",
    var gender: String = "",
    var birth: String = "",
    var neutering: String = "",
    var vaccination: String = "",
    var weight: String = "",
    var feature: String = "",
    var creationTime: Long = 0,
    var totalWalkInfo: TotalWalkInfo = TotalWalkInfo()
) : Serializable