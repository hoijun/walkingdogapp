package com.tulmunchi.walkingdogapp.data.model

data class WeatherRequestDto(
    val serviceKey: String,
    val numOfRows: Int,
    val pageNo: Int,
    val dataType: String,
    val baseDate: String,
    val baseTime: String,
    val nx: Int,
    val ny: Int
)