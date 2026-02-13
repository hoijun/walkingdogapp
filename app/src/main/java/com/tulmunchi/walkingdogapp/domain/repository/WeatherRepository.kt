package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.WeatherRequest
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse

interface WeatherRepository {
    /**
     * Get weather forecast
     */
    suspend fun getWeatherForecast(weatherRequest: WeatherRequest): Result<WeatherResponse>
}