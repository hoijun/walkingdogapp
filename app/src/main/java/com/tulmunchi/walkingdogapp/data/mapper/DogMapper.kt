package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.DogDto
import com.tulmunchi.walkingdogapp.domain.model.Dog

/**
 * Mapper between DogDto and Dog domain model
 */
object DogMapper {
    fun toDomain(dto: DogDto): Dog = Dog(
        name = dto.name,
        breed = dto.breed,
        gender = dto.gender,
        birth = dto.birth,
        neutering = dto.neutering,
        vaccination = dto.vaccination,
        weight = dto.weight,
        feature = dto.feature,
        creationTime = dto.creationTime,
        dogWithStats = WalkStatsMapper.toDomain(dto.totalWalkInfo)
    )

    fun toDto(domain: Dog): DogDto = DogDto(
        name = domain.name,
        breed = domain.breed,
        gender = domain.gender,
        birth = domain.birth,
        neutering = domain.neutering,
        vaccination = domain.vaccination,
        weight = domain.weight,
        feature = domain.feature,
        creationTime = domain.creationTime,
        totalWalkInfo = WalkStatsMapper.toDto(domain.dogWithStats)
    )
}
