package com.tulmunchi.walkingdogapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.mapper.DogMapper
import com.tulmunchi.walkingdogapp.data.mapper.WalkRecordMapper
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseDogDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseStorageDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSource
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import javax.inject.Inject


/**
 * Implementation of DogRepository
 */
class DogRepositoryImpl @Inject constructor(
    private val firebaseDogDataSource: FirebaseDogDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val firebaseWalkDataSource: FirebaseWalkDataSource,
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

    override suspend fun getDog(name: String): Result<Dog> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseDogDataSource.getDog(uid, name)
            .map { DogMapper.toDomain(it) }
    }

    override suspend fun updateDog(
        oldName: String,
        dog: Dog,
        imageUriString: String?,
        walkRecords: List<WalkRecord>,
        existingDogNames: List<String>
    ): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }

        return try {
            val isNameChanged = oldName.isNotEmpty() && oldName != dog.name

            // Step 1: Update dog data (if name changed, delete old and create new)
            val dogDto = DogMapper.toDto(dog)
            firebaseDogDataSource.updateDog(uid, oldName, dogDto).getOrThrow()

            // Step 2: Transfer walk records to new dog name
            if (walkRecords.isNotEmpty()) {
                val walkRecordDtos = walkRecords.map { WalkRecordMapper.toDto(it) }
                firebaseWalkDataSource.saveWalkRecords(uid, dog.name, walkRecordDtos).getOrThrow()
            }

            // Step 3: Handle image
            if (imageUriString != null) {
                // Upload new image
                val imageUri = Uri.parse(imageUriString)
                firebaseStorageDataSource.uploadDogImage(uid, dog.name, imageUri).getOrThrow()
            } else if (isNameChanged) {
                // Copy existing image to new name
                try {
                    firebaseStorageDataSource.copyDogImage(uid, oldName, dog.name).getOrThrow()
                } catch (e: Exception) {
                    // Image copy failed (maybe no existing image), continue
                }
            }

            // Step 4: Delete old image if name changed
            if (isNameChanged && !existingDogNames.contains(dog.name)) {
                try {
                    firebaseStorageDataSource.deleteDogImage(uid, oldName).getOrThrow()
                } catch (e: Exception) {
                    // Old image deletion failed, continue
                }
            }

            Result.success(Unit)
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
