package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord

/**
 * Repository interface for dog-related operations
 */
interface DogRepository {
    /**
     * Get all dogs
     */
    suspend fun getAllDogs(): Result<List<Dog>>

    /**
     * Get a specific dog by name
     */
    suspend fun getDog(name: String): Result<Dog>

    /**
     * Update existing dog information
     * @param oldName Previous dog name (empty string for new dog)
     * @param dog Updated dog information
     * @param imageUriString New image URI (null to keep existing image)
     * @param walkRecords Walk records to transfer to new dog name
     * @param existingDogNames List of existing dog names to check for duplicates
     */
    suspend fun updateDog(
        oldName: String,
        dog: Dog,
        imageUriString: String?,
        walkRecords: List<WalkRecord>,
        existingDogNames: List<String>
    ): Result<Unit>

    /**
     * Delete a dog
     */
    suspend fun deleteDog(name: String): Result<Unit>

    /**
     * Get dog image URI as string
     */
    suspend fun getDogImage(dogName: String): Result<String>

    /**
     * Get all dog images as a map (dog name -> image URI string)
     */
    suspend fun getAllDogImages(): Result<Map<String, String>>

    /**
     * Get all dogs with their images (dog -> image URI string)
     */
    suspend fun getAllDogsWithImages(): Result<Map<Dog, String>>
}
