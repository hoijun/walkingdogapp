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
        calories = dto.calories,
        poopCoordinates = CoordinateMapper.toDomainList(dto.poopCoordinates),
        memoCoordinates = dto.memoCoordinates.mapValues { (_, value) -> CoordinateMapper.toDomain(value) },
        walkCoordinates = CoordinateMapper.toDomainList(dto.walkCoordinates),
        collections = dto.collections
    )

    fun toDto(domain: WalkRecord): WalkRecordDto = WalkRecordDto(
        day = domain.day,
        startTime = domain.startTime,
        endTime = domain.endTime,
        distance = domain.distance,
        time = domain.time,
        calories = domain.calories,
        poopCoordinates = CoordinateMapper.toDtoList(domain.poopCoordinates),
        memoCoordinates = domain.memoCoordinates.mapValues { (_, value) -> CoordinateMapper.toDto(value) },
        walkCoordinates = CoordinateMapper.toDtoList(domain.walkCoordinates),
        collections = domain.collections
    )

    fun toDomainList(dtos: List<WalkRecordDto>): List<WalkRecord> = dtos.map { toDomain(it) }
}
