package com.tulmunchi.walkingdogapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.mapper.DogMapper
import com.tulmunchi.walkingdogapp.data.mapper.WalkStatsMapper
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseDogDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseStorageDataSource
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DogWithStats
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject

/**
 * Implementation of DogRepository
 */
class DogRepositoryImpl @Inject constructor(
    private val firebaseDogDataSource: FirebaseDogDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val auth: FirebaseAuth,
    private val networkChecker: NetworkChecker
) : DogRepository {

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override suspend fun getAllDogs(): Result<List<Dog>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseDogDataSource.getAllDogs(uid)
            .map { dogs -> dogs.map { DogMapper.toDomain(it) } }
    }

    override suspend fun getAllDogsWithStats(): Result<List<DogWithStats>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseDogDataSource.getAllDogs(uid)
            .map { dogs -> dogs.map { DogMapper.toDomainWithStats(it) } }
    }

    override suspend fun getDog(name: String): Result<Dog> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseDogDataSource.getDog(uid, name)
            .map { DogMapper.toDomain(it) }
    }

    override suspend fun saveDog(dog: Dog, imageUriString: String?): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            // Upload image if provided
            imageUriString?.let {
                val imageUri = Uri.parse(it)
                firebaseStorageDataSource.uploadDogImage(uid, dog.name, imageUri)
            }

            // Save dog data
            val dogDto = DogMapper.toDto(dog)
            firebaseDogDataSource.saveDog(uid, dogDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDog(oldName: String, dog: Dog, imageUriString: String?): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            // Upload new image if provided
            imageUriString?.let {
                val imageUri = Uri.parse(it)
                firebaseStorageDataSource.uploadDogImage(uid, dog.name, imageUri)
            }

            // Update dog data
            val dogDto = DogMapper.toDto(dog)
            firebaseDogDataSource.updateDog(uid, oldName, dogDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDog(name: String): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            // Delete image
            firebaseStorageDataSource.deleteDogImage(uid, name)

            // Delete dog data
            firebaseDogDataSource.deleteDog(uid, name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDogImage(dogName: String): Result<String> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseStorageDataSource.getDogImageUrl(uid, dogName)
    }

    override suspend fun getAllDogImages(): Result<Map<String, String>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return getAllDogs().fold(
            onSuccess = { dogs ->
                val dogNames = dogs.map { it.name }
                firebaseStorageDataSource.getAllDogImageUrls(uid, dogNames)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun getAllDogsWithImages(): Result<Map<Dog, String>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return try {
            val dogsResult = getAllDogs()
            val imagesResult = getAllDogImages()

            if (dogsResult.isSuccess && imagesResult.isSuccess) {
                val dogs = dogsResult.getOrThrow()
                val images = imagesResult.getOrThrow()

                val dogImageMap = dogs.associateWith { dog ->
                    images[dog.name] ?: ""
                }

                Result.success(dogImageMap)
            } else {
                Result.failure(Exception("Failed to load dogs or images"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
