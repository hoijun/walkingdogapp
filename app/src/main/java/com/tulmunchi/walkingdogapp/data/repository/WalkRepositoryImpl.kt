package com.tulmunchi.walkingdogapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.mapper.CoordinateMapper
import com.tulmunchi.walkingdogapp.data.mapper.WalkRecordMapper
import com.tulmunchi.walkingdogapp.data.mapper.WalkStatsMapper
import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseCollectionDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSource
import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Implementation of WalkRepository
 */
class WalkRepositoryImpl @Inject constructor(
    private val firebaseWalkDataSource: FirebaseWalkDataSource,
    private val firebaseUserDataSource: FirebaseUserDataSource,
    private val firebaseCollectionDataSource: FirebaseCollectionDataSource,
    private val auth: FirebaseAuth,
    private val networkChecker: NetworkChecker
) : WalkRepository {

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override suspend fun saveWalkRecord(
        dogNames: List<String>,
        walkRecord: WalkRecord
    ): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            // Save record for each dog
            for (dogName in dogNames) {
                firebaseWalkDataSource.saveWalkRecord(uid, dogName, WalkRecordMapper.toDto(walkRecord)).getOrThrow()
            }

            firebaseCollectionDataSource.updateCollection(uid, walkRecord.collections).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllWalkHistory(): Result<Map<String, List<WalkRecord>>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseWalkDataSource.getAllWalkHistory(uid)
            .map { historyMap ->
                historyMap.mapValues { (_, records) ->
                    WalkRecordMapper.toDomainList(records)
                }
            }
    }

    override suspend fun getTotalWalkStats(): Result<WalkStats> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }

        return firebaseUserDataSource.getTotalWalkStats(uid)
            .map { WalkStatsMapper.toDomain(it) }
    }
}
