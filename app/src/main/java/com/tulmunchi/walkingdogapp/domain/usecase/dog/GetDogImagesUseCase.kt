package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for getting all dog images
 */
class GetDogImagesUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(): Result<Map<String, String>> {
        return dogRepository.getAllDogImages()
    }
}
