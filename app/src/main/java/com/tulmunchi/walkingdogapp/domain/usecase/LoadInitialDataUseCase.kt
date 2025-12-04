package com.tulmunchi.walkingdogapp.domain.usecase

import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.InitialData
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.GetAllAlarmsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.collection.GetAllCollectionsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.GetAllDogsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.GetDogImagesUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.user.GetUserInfoUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.walk.GetAllWalkHistoryUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.walk.GetTotalWalkStatsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Composite use case for loading all initial data in parallel
 * This is the main use case for the "load all data at start" approach
 */
class LoadInitialDataUseCase @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getAllDogsUseCase: GetAllDogsUseCase,
    private val getDogImagesUseCase: GetDogImagesUseCase,
    private val getTotalWalkStatsUseCase: GetTotalWalkStatsUseCase,
    private val getAllWalkHistoryUseCase: GetAllWalkHistoryUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val getAllAlarmsUseCase: GetAllAlarmsUseCase
) {
    suspend operator fun invoke(loadImages: Boolean = true): Result<InitialData> {
        return try {
            coroutineScope {
                // Launch all data loading operations in parallel
                val userDeferred = async { getUserInfoUseCase() }
                val dogsDeferred = async { getAllDogsUseCase() }
                val imagesDeferred = if (loadImages) {
                    async { getDogImagesUseCase() }
                } else {
                    null
                }
                val statsDeferred = async { getTotalWalkStatsUseCase() }
                val walkHistoryDeferred = async { getAllWalkHistoryUseCase() }
                val collectionsDeferred = async { getAllCollectionsUseCase() }
                val alarmsDeferred = async { getAllAlarmsUseCase() }

                // Wait for all results
                val user: User? = userDeferred.await().getOrNull()
                val dogs: List<Dog> = dogsDeferred.await().getOrElse { emptyList() }
                val dogImages: Map<String, String> = imagesDeferred?.await()?.getOrElse { emptyMap() } ?: emptyMap()
                val totalWalkStats: WalkStats = statsDeferred.await().getOrElse { WalkStats() }
                val walkHistory: Map<String, List<WalkRecord>> = walkHistoryDeferred.await().getOrElse { emptyMap() }
                val collections: Map<String, Boolean> = collectionsDeferred.await().getOrElse { emptyMap() }
                val alarms: List<Alarm> = alarmsDeferred.await().getOrElse { emptyList() }

                Result.success(
                    InitialData(
                        user = user,
                        dogs = dogs,
                        dogImages = dogImages,
                        totalWalkStats = totalWalkStats,
                        walkHistory = walkHistory,
                        collections = collections,
                        alarms = alarms
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(DomainError.UnknownError(e.message ?: "알 수 없는 오류가 발생했습니다"))
        }
    }
}
