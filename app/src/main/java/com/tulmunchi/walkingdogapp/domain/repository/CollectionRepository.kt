package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.Collection

/**
 * Repository interface for collection-related operations
 */
interface CollectionRepository {
    /**
     * Get all collections with their ownership status
     */
    suspend fun getAllCollections(): Result<Map<String, Boolean>>

    /**
     * Update collection ownership
     */
    suspend fun updateCollections(collections: Map<String, Boolean>): Result<Unit>

    /**
     * Check if a specific collection is owned
     */
    suspend fun isCollectionOwned(collectionNum: String): Result<Boolean>
}
