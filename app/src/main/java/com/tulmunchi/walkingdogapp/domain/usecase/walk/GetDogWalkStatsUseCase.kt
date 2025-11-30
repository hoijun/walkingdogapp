package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * Use case for getting walk statistics for a specific dog
 */
class GetDogWalkStatsUseCase @Inject constructor(
    private val walkRepository: WalkRepository
) {
    suspend operator fun invoke(dogName: String): Result<WalkStats> {
        if (dogName.isBlank()) {
            return Result.failure(DomainError.ValidationError("강아지 이름이 올바르지 않습니다"))
        }

        return walkRepository.getDogWalkStats(dogName)
    }
}
