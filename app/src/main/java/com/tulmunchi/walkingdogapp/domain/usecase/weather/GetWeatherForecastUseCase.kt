package com.tulmunchi.walkingdogapp.domain.usecase.weather

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WeatherRequest
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import com.tulmunchi.walkingdogapp.domain.repository.WeatherRepository
import com.tulmunchi.walkingdogapp.domain.util.GpsGridConverter
import jakarta.inject.Inject

class GetWeatherForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(baseDate: String, baseTime: String, lat: Double, lon: Double): Result<WeatherResponse> {
        val gridXY = GpsGridConverter.convertGRIDBetweenGPS(GpsGridConverter.MODE_GRID, lat, lon)
        val nx = gridXY.x ?: return Result.failure(DomainError.WeatherError())
        val ny = gridXY.y ?: return Result.failure(DomainError.WeatherError())

        return weatherRepository.getWeatherForecast(
            WeatherRequest(
                baseDate = baseDate,
                baseTime = baseTime,
                nx = nx,
                ny = ny
            )
        )
    }
}