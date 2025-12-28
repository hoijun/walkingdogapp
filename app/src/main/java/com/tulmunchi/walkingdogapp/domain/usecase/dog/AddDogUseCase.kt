package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for Adding a dog
 */
class AddDogUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(dog: Dog, imageUriString: String?): Result<Unit> {
        if (dog.name.isBlank()) {
            return Result.failure(DomainError.ValidationError("강아지 이름이 올바르지 않습니다"))
        }

        return dogRepository.addDog(dog, imageUriString)
    }
}