package com.tulmunchi.walkingdogapp.domain.usecase.collection

import com.tulmunchi.walkingdogapp.domain.repository.CollectionRepository
import javax.inject.Inject

/**
 * Use case for getting all collections with ownership status
 */
class GetAllCollectionsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(): Result<Map<String, Boolean>> {
        return collectionRepository.getAllCollections()
    }
}
