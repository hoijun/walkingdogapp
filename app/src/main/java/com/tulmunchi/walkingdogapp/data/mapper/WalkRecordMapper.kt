package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord

/**
 * Mapper between WalkRecordDto and WalkRecord domain model
 */
object WalkRecordMapper {
    fun toDomain(dto: WalkRecordDto): WalkRecord = WalkRecord(
        day = dto.day,
        startTime = dto.startTime,
        endTime = dto.endTime,
        distance = dto.distance,
        time = dto.time,
        coords = CoordinateMapper.toDomainList(dto.coords),
        collections = dto.collections
    )

    fun toDto(domain: WalkRecord): WalkRecordDto = WalkRecordDto(
        day = domain.day,
        startTime = domain.startTime,
        endTime = domain.endTime,
        distance = domain.distance,
        time = domain.time,
        coords = CoordinateMapper.toDtoList(domain.coords),
        collections = domain.collections
    )

    fun toDomainList(dtos: List<WalkRecordDto>): List<WalkRecord> = dtos.map { toDomain(it) }

    fun toDtoList(domains: List<WalkRecord>): List<WalkRecordDto> = domains.map { toDto(it) }
}
