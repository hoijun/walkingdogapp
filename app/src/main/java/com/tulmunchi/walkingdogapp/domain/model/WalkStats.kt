package com.tulmunchi.walkingdogapp.domain.model

import java.io.Serializable

/**
 * Domain model representing walk statistics
 */
data class WalkStats(
    val distance: Float = 0f,
    val time: Int = 0
) : Serializable
