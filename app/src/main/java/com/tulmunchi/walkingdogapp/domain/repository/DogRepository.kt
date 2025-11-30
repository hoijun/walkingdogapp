package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DogWithStats

/**
 * Repository interface for dog-related operations
 */
interface DogRepository {
    /**
     * Get all dogs
     */
    suspend fun getAllDogs(): Result<List<Dog>>

    /**
     * Get all dogs with their walk statistics
     */
    suspend fun getAllDogsWithStats(): Result<List<DogWithStats>>

    /**
     * Get a specific dog by name
     */
    suspend fun getDog(name: String): Result<Dog>

    /**
     * Save a new dog (with optional image)
     * @param imageUriString Optional image URI as string (file path or content URI)
     */
    suspend fun saveDog(dog: Dog, imageUriString: String?): Result<Unit>

    /**
     * Update existing dog information
     * @param imageUriString Optional image URI as string (file path or content URI)
     */
    suspend fun updateDog(oldName: String, dog: Dog, imageUriString: String?): Result<Unit>

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
