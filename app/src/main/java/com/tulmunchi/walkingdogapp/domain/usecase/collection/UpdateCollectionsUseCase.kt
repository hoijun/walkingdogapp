package com.tulmunchi.walkingdogapp.domain.usecase.collection

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.CollectionRepository
import javax.inject.Inject

/**
 * Use case for updating collection ownership
 */
class UpdateCollectionsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(collections: Map<String, Boolean>): Result<Unit> {
        if (collections.isEmpty()) {
            return Result.failure(DomainError.ValidationError("업데이트할 컬렉션이 없습니다"))
        }

        return collectionRepository.updateCollections(collections)
    }
}
