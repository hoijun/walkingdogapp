package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model for weather forecast request
 */
data class WeatherRequest(
    val baseDate: String,
    val baseTime: String,
    val nx: Int,
    val ny: Int
)
