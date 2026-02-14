package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.WeatherResponseDto
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeatherResponseMapperTest {

    @Test
    fun `toDomain은 PTY와 SKY가 모두 있으면 WeatherResponse를 반환한다`() {
        val dto = weatherDto(
            item(category = "PTY", fcstTime = "0000", fcstValue = "1"),
            item(category = "SKY", fcstTime = "0000", fcstValue = "3")
        )

        val result = WeatherResponseMapper.toDomain(dto)

        assertNotNull(result)
        assertEquals("1", result.pty)
        assertEquals("3", result.sky)
    }

    @Test
    fun `toDomain은 PTY 또는 SKY가 없으면 null을 반환한다`() {
        val dto = weatherDto(
            item(category = "PTY", fcstTime = "0000", fcstValue = "1")
        )

        val result = WeatherResponseMapper.toDomain(dto)

        assertNull(result)
    }

    @Test
    fun `toDomainList는 PTY와 SKY가 모두 있는 시간대만 반환한다`() {
        val dto = weatherDto(
            item(category = "PTY", fcstTime = "0000", fcstValue = "0"),
            item(category = "SKY", fcstTime = "0000", fcstValue = "1"),
            item(category = "PTY", fcstTime = "0100", fcstValue = "1"),
            item(category = "PTY", fcstTime = "0200", fcstValue = "0"),
            item(category = "SKY", fcstTime = "0200", fcstValue = "4")
        )

        val result = WeatherResponseMapper.toDomainList(dto)

        assertEquals(2, result.size)
        assertEquals("0", result[0].pty)
        assertEquals("1", result[0].sky)
        assertEquals("0", result[1].pty)
        assertEquals("4", result[1].sky)
    }

    private fun weatherDto(vararg items: WeatherResponseDto.Item): WeatherResponseDto {
        return WeatherResponseDto(
            response = WeatherResponseDto.Response(
                body = WeatherResponseDto.Body(
                    items = WeatherResponseDto.Items(
                        item = items.toList()
                    )
                )
            )
        )
    }

    private fun item(category: String, fcstTime: String, fcstValue: String): WeatherResponseDto.Item {
        return WeatherResponseDto.Item(
            category = category,
            fcstTime = fcstTime,
            fcstValue = fcstValue
        )
    }
}
