package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * Use case for saving walk record
 */
class SaveWalkRecordUseCase @Inject constructor(
    private val walkRepository: WalkRepository
) {
    suspend operator fun invoke(
        dogNames: List<String>,
        startTime: String,
        distance: Float,
        time: Int,
        coords: List<Coordinate>,
        collections: List<String>
    ): Result<Unit> {
        // Validate walk data
        if (dogNames.isEmpty()) {
            return Result.failure(DomainError.ValidationError("산책할 강아지를 선택해주세요"))
        }

        if (distance <= 0f) {
            return Result.failure(DomainError.ValidationError("산책 거리가 올바르지 않습니다"))
        }

        return walkRepository.saveWalkRecord(dogNames, startTime, distance, time, coords, collections)
    }
}
