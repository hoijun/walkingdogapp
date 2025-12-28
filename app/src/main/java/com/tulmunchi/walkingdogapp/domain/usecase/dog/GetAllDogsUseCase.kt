package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for getting all dogs
 */
class GetAllDogsUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(): Result<List<Dog>> {
        return dogRepository.getAllDogs()
    }
}
