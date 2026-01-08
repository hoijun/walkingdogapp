package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
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
        walkRecord: WalkRecord
    ): Result<Unit> {
        // Validate walk data
        if (dogNames.isEmpty()) {
            return Result.failure(DomainError.ValidationError("산책할 강아지를 선택해주세요"))
        }

        return walkRepository.saveWalkRecord(dogNames, walkRecord)
    }
}
