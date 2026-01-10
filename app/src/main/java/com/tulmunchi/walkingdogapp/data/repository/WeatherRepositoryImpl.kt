package com.tulmunchi.walkingdogapp.data.repository

import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.mapper.WeatherRequestMapper
import com.tulmunchi.walkingdogapp.data.mapper.WeatherResponseMapper
import com.tulmunchi.walkingdogapp.data.source.remote.WeatherDataSource
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WeatherRequest
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import com.tulmunchi.walkingdogapp.domain.repository.WeatherRepository
import jakarta.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val weatherDataSource: WeatherDataSource,
    private val networkChecker: NetworkChecker
) : WeatherRepository
{
    override suspend fun getWeatherForecast(weatherRequest: WeatherRequest): Result<WeatherResponse> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            // Domain model을 Data DTO로 변환
            val walkResponseDto = weatherDataSource.getWeatherForecast(WeatherRequestMapper.toDto(weatherRequest))
            val walkResponse = WeatherResponseMapper.toDomain(walkResponseDto) ?: return Result.failure(DomainError.WeatherError())
            Result.success(walkResponse)
        } catch (e: Exception) {
            Result.failure(DomainError.WeatherError())
        }
    }
}