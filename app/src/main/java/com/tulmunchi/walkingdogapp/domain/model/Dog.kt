package com.tulmunchi.walkingdogapp.domain.model

import java.io.Serializable

/**
 * Domain model representing a dog
 */
data class Dog(
    var name: String = "",
    val breed: String = "",
    val gender: String = "",
    val birth: String = "",
    val neutering: String = "",
    val vaccination: String = "",
    var weight: String = "",
    var feature: String = "",
    val creationTime: Long = 0L,
    val dogWithStats: WalkStats = WalkStats()
) : Serializable
