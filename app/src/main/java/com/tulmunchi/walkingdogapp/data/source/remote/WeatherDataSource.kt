package com.tulmunchi.walkingdogapp.data.source.remote

import com.tulmunchi.walkingdogapp.data.model.WeatherRequestDto
import com.tulmunchi.walkingdogapp.data.model.WeatherResponseDto
import com.tulmunchi.walkingdogapp.data.service.WeatherApiService
import javax.inject.Inject

interface WeatherDataSource {
    suspend fun getWeatherForecast(weatherRequestDto: WeatherRequestDto): WeatherResponseDto
}

class WeatherDataSourceImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
) : WeatherDataSource {
    override suspend fun getWeatherForecast(weatherRequestDto: WeatherRequestDto): WeatherResponseDto {
        return weatherApiService.getForecast(
            serviceKey = weatherRequestDto.serviceKey,
            numOfRows = weatherRequestDto.numOfRows,
            pageNo = weatherRequestDto.pageNo,
            dataType = weatherRequestDto.dataType,
            baseDate = weatherRequestDto.baseDate,
            baseTime = weatherRequestDto.baseTime,
            nx = weatherRequestDto.nx,
            ny = weatherRequestDto.ny
        )
    }
}
