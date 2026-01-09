package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.WeatherResponseDto
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherResponseMapper {
    fun toDomain(dto: WeatherResponseDto): WeatherResponse? {
        // 현재 시간 가져오기 (HHmm 형식, 예: "0425")
        val currentTime = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
        
        val items = dto.response.body.items.item
        
        // 사용 가능한 모든 예보 시간 목록 가져오기
        val availableTimes = items.map { it.fcstTime }.distinct().sorted()
        
        // 현재 시간 이하의 가장 가까운 예보 시간 찾기 (현재 날씨 체감)
        val targetTime = availableTimes
            .filter { it <= currentTime }  // 현재 시간 이하만 필터링
            .maxOrNull()                   // 가장 최근 과거 시간
            ?: availableTimes.firstOrNull() // 없으면 첫 예보 사용 (새벽 시간대)
            ?: return null
        
        // 해당 시간의 항목들만 필터링
        val targetTimeItems = items.filter { it.fcstTime == targetTime }
        
        // PTY(강수형태)와 SKY(하늘상태) 카테고리만 필터링
        val ptyItem = targetTimeItems.find { it.category == "PTY" }
        val skyItem = targetTimeItems.find { it.category == "SKY" }
        
        // 둘 다 있을 때만 WeatherResponse 생성
        return if (ptyItem != null && skyItem != null) {
            WeatherResponse(
                pty = ptyItem.fcstValue,
                sky = skyItem.fcstValue
            )
        } else {
            null
        }
    }
    
    // 여러 시간대의 PTY와 SKY 데이터를 가져오는 방법
    fun toDomainList(dto: WeatherResponseDto): List<WeatherResponse> {
        val items = dto.response.body.items.item
        
        // fcstTime별로 그룹화
        val groupedByTime = items.groupBy { it.fcstTime }
        
        // 각 시간대별로 PTY와 SKY를 매칭
        return groupedByTime.mapNotNull { (_, timeItems) ->
            val ptyItem = timeItems.find { it.category == "PTY" }
            val skyItem = timeItems.find { it.category == "SKY" }
            
            if (ptyItem != null && skyItem != null) {
                WeatherResponse(
                    pty = ptyItem.fcstValue,
                    sky = skyItem.fcstValue
                )
            } else {
                null
            }
        }
    }
}