package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.WalkStatsDto
import com.tulmunchi.walkingdogapp.domain.model.WalkStats

/**
 * Mapper between WalkStatsDto and WalkStats domain model
 */
object WalkStatsMapper {
    fun toDomain(dto: WalkStatsDto): WalkStats = WalkStats(
        distance = dto.distance,
        time = dto.time
    )

    fun toDto(domain: WalkStats): WalkStatsDto = WalkStatsDto(
        distance = domain.distance,
        time = domain.time
    )
}
