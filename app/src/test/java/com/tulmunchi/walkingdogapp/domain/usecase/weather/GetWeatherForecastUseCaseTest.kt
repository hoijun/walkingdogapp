package com.tulmunchi.walkingdogapp.domain.usecase.weather

import com.tulmunchi.walkingdogapp.domain.model.WeatherRequest
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import com.tulmunchi.walkingdogapp.domain.repository.WeatherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetWeatherForecastUseCaseTest {

    @Test
    fun `격자 변환 후 repository를 호출하고 결과를 반환한다`() = runBlocking {
        val repository = FakeWeatherRepository(
            result = Result.success(WeatherResponse(sky = "1", pty = "0"))
        )
        val useCase = GetWeatherForecastUseCase(repository)

        val result = useCase(
            baseDate = "20260214",
            baseTime = "0900",
            lat = 37.5665,
            lon = 126.9780
        )

        assertTrue(result.isSuccess)
        assertNotNull(repository.lastRequest)
        assertEquals("20260214", repository.lastRequest?.baseDate)
        assertEquals("0900", repository.lastRequest?.baseTime)
    }

    @Test
    fun `repository 실패를 그대로 반환한다`() = runBlocking {
        val repository = FakeWeatherRepository(
            result = Result.failure(IllegalStateException("weather fail"))
        )
        val useCase = GetWeatherForecastUseCase(repository)

        val result = useCase(
            baseDate = "20260214",
            baseTime = "0900",
            lat = 37.5665,
            lon = 126.9780
        )

        assertTrue(result.isFailure)
    }

    private class FakeWeatherRepository(
        private val result: Result<WeatherResponse>
    ) : WeatherRepository {
        var lastRequest: WeatherRequest? = null

        override suspend fun getWeatherForecast(weatherRequest: WeatherRequest): Result<WeatherResponse> {
            lastRequest = weatherRequest
            return result
        }
    }
}
