package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Use case for updating dog information
 */
class UpdateDogUseCase @Inject constructor(
    private val dogRepository: DogRepository
) {
    suspend operator fun invoke(
        oldName: String,
        dog: Dog,
        imageUriString: String?,
        walkRecords: List<WalkRecord>,
        existingDogNames: List<String>
    ): Result<Unit> {
        // Validate dog data
        if (dog.name.isBlank()) {
            return Result.failure(DomainError.ValidationError("강아지 이름을 입력해주세요"))
        }

        return dogRepository.updateDog(oldName, dog, imageUriString, walkRecords, existingDogNames)
    }
}
