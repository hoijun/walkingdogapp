package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.BuildConfig
import com.tulmunchi.walkingdogapp.data.model.WeatherRequestDto
import com.tulmunchi.walkingdogapp.domain.model.WeatherRequest

object WeatherRequestMapper {
    fun toDto(domain: WeatherRequest): WeatherRequestDto {
        return WeatherRequestDto(
            serviceKey = BuildConfig.Weather_API_KEY,
            numOfRows = 24,
            pageNo = 1,
            dataType = "JSON",
            baseDate = domain.baseDate,
            baseTime = domain.baseTime,
            nx = domain.nx,
            ny = domain.ny
        )
    }
}