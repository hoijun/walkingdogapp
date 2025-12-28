package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * Use case for getting total walk statistics for all dogs
 */
class GetTotalWalkStatsUseCase @Inject constructor(
    private val walkRepository: WalkRepository
) {
    suspend operator fun invoke(): Result<WalkStats> {
        return walkRepository.getTotalWalkStats()
    }
}
