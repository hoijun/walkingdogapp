package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * Use case for getting all walk history for all dogs
 */
class GetAllWalkHistoryUseCase @Inject constructor(
    private val walkRepository: WalkRepository
) {
    suspend operator fun invoke(): Result<Map<String, List<WalkRecord>>> {
        return walkRepository.getAllWalkHistory()
    }
}
