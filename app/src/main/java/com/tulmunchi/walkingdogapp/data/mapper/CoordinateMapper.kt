package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.CoordinateDto
import com.tulmunchi.walkingdogapp.domain.model.Coordinate

/**
 * Mapper between CoordinateDto and Coordinate domain model
 */
object CoordinateMapper {
    fun toDomain(dto: CoordinateDto): Coordinate = Coordinate(
        latitude = dto.latitude,
        longitude = dto.longitude
    )

    fun toDto(domain: Coordinate): CoordinateDto = CoordinateDto(
        latitude = domain.latitude,
        longitude = domain.longitude
    )

    fun toDomainList(dtos: List<CoordinateDto>): List<Coordinate> = dtos.map { toDomain(it) }

    fun toDtoList(domains: List<Coordinate>): List<CoordinateDto> = domains.map { toDto(it) }
}
