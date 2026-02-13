package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats

/**
 * Repository interface for walk-related operations
 */
interface WalkRepository {
    /**
     * Save walk record for multiple dogs
     */
    suspend fun saveWalkRecord(
        dogNames: List<String>,
        walkRecord: WalkRecord
    ): Result<Unit>

    /**
     * Get all walk history for all dogs
     */
    suspend fun getAllWalkHistory(): Result<Map<String, List<WalkRecord>>>

    /**
     * Get total walk statistics for all dogs
     */
    suspend fun getTotalWalkStats(): Result<WalkStats>
}
