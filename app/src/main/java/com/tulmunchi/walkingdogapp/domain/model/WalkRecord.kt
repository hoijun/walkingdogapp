package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing a walk record
 */
data class WalkRecord(
    val day: String,
    val startTime: String,
    val endTime: String,
    val distance: Float,
    val time: Int,
    val coords: List<Coordinate>,
    val collections: List<String>
)
