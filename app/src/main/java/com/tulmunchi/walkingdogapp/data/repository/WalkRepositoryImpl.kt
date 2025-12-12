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
        startTime: String,
        distance: Float,
        time: Int,
        coords: List<Coordinate>,
        collections: List<String>
    ): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val recordDto = WalkRecordDto(
                day = day,
                startTime = startTime,
                endTime = currentTime,
                distance = distance,
                time = time,
                coords = CoordinateMapper.toDtoList(coords),
                collections = collections
            )

            // Save record for each dog
            for (dogName in dogNames) {
                firebaseWalkDataSource.saveWalkRecord(uid, dogName, recordDto).getOrThrow()
            }

            firebaseCollectionDataSource.updateCollection(uid, collections).getOrThrow()

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
