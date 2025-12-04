package com.tulmunchi.walkingdogapp.data.source.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firebase Storage DataSource for image operations
 */
interface FirebaseStorageDataSource {
    suspend fun uploadDogImage(uid: String, dogName: String, imageUri: Uri): Result<String>
    suspend fun getDogImageUrl(uid: String, dogName: String): Result<String>
    suspend fun getAllDogImageUrls(uid: String, dogNames: List<String>): Result<Map<String, String>>
    suspend fun deleteDogImage(uid: String, dogName: String): Result<Unit>
    suspend fun copyDogImage(uid: String, oldName: String, newName: String): Result<Unit>
}

class FirebaseStorageDataSourceImpl @Inject constructor(
    private val storage: FirebaseStorage
) : FirebaseStorageDataSource {

    override suspend fun uploadDogImage(uid: String, dogName: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.getReference(uid).child("images").child(dogName)
            storageRef.putFile(imageUri).await()

            // Return the download URL as string
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDogImageUrl(uid: String, dogName: String): Result<String> {
        return try {
            val storageRef = storage.getReference(uid).child("images").child(dogName)
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllDogImageUrls(uid: String, dogNames: List<String>): Result<Map<String, String>> {
        return try {
            coroutineScope {
                // 병렬 처리로 성능 향상
                val results = dogNames.map { dogName ->
                    async {
                        dogName to getDogImageUrl(uid, dogName).getOrNull()
                    }
                }.awaitAll()

                val imageMap = results
                    .filter { it.second != null }
                    .associate { it.first to it.second!! }

                Result.success(imageMap)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDogImage(uid: String, dogName: String): Result<Unit> {
        return try {
            val storageRef = storage.getReference(uid).child("images").child(dogName)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copyDogImage(uid: String, oldName: String, newName: String): Result<Unit> {
        return try {
            val oldRef = storage.getReference(uid).child("images").child(oldName)
            val newRef = storage.getReference(uid).child("images").child(newName)

            // Download the old image
            val bytes = oldRef.getBytes(Long.MAX_VALUE).await()

            // Upload to new location
            newRef.putBytes(bytes).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
