package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for saving a new dog
 */
class SaveDogUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(dog: Dog, imageUriString: String?): Result<Unit> {
        // Validate dog data
        if (dog.name.isBlank()) {
            return Result.failure(DomainError.ValidationError("강아지 이름을 입력해주세요"))
        }

        return dogRepository.saveDog(dog, imageUriString)
    }
}
