package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Firebase DataSource for Collection operations
 */
interface FirebaseCollectionDataSource {
    suspend fun getAllCollections(uid: String): Result<Map<String, Boolean>>
    suspend fun updateCollections(uid: String, collections: Map<String, Boolean>): Result<Unit>
}

class FirebaseCollectionDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : FirebaseCollectionDataSource {

    override suspend fun getAllCollections(uid: String): Result<Map<String, Boolean>> = suspendCoroutine { cont ->
        database.getReference("Users").child(uid).child("collection")
            .get()
            .addOnSuccessListener { snapshot ->
                val collections = mutableMapOf<String, Boolean>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        val value = child.getValue(Boolean::class.java) ?: false
                        collections[key] = value
                    }
                }
                cont.resume(Result.success(collections))
            }
            .addOnFailureListener { exception ->
                cont.resume(Result.failure(exception))
            }
    }

    override suspend fun updateCollections(uid: String, collections: Map<String, Boolean>): Result<Unit> {
        return try {
            database.getReference("Users").child(uid).child("collection")
                .setValue(collections)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
