package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.model.UserDto
import com.tulmunchi.walkingdogapp.domain.model.User

/**
 * Mapper between UserDto and User domain model
 */
object UserMapper {
    fun toDomain(dto: UserDto): User = User(
        email = dto.email,
        name = dto.name,
        gender = dto.gender,
        birth = dto.birth
    )

    fun toDto(domain: User): UserDto = UserDto(
        email = domain.email,
        name = domain.name,
        gender = domain.gender,
        birth = domain.birth
    )
}
