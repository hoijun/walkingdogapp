package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.DogWithStats
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for getting all dogs with their walk statistics
 */
class GetAllDogsWithStatsUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(): Result<List<DogWithStats>> {
        return dogRepository.getAllDogsWithStats()
    }
}
