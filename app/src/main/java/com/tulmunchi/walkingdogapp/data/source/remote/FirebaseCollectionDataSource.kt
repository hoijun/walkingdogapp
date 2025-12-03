package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firebase DataSource for Collection operations
 */
interface FirebaseCollectionDataSource {
    suspend fun getAllCollections(uid: String): Result<Map<String, Boolean>>
}

class FirebaseCollectionDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : FirebaseCollectionDataSource {

    override suspend fun getAllCollections(uid: String): Result<Map<String, Boolean>> {
        return try {
            val snapshot = database.getReference("Users").child(uid).child("collection")
                .get()
                .await()
            val collections = mutableMapOf<String, Boolean>()
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.getValue(Boolean::class.java) ?: false
                    collections[key] = value
                }
            }
            Result.success(collections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
