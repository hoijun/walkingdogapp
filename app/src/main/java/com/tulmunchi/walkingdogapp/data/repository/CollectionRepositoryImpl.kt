package com.tulmunchi.walkingdogapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseCollectionDataSource
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.CollectionRepository
import javax.inject.Inject

/**
 * Implementation of CollectionRepository
 */
class CollectionRepositoryImpl @Inject constructor(
    private val firebaseCollectionDataSource: FirebaseCollectionDataSource,
    private val auth: FirebaseAuth,
    private val networkChecker: NetworkChecker
) : CollectionRepository {

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override suspend fun getAllCollections(): Result<Map<String, Boolean>> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseCollectionDataSource.getAllCollections(uid)
    }

    override suspend fun isCollectionOwned(collectionNum: String): Result<Boolean> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return getAllCollections()
            .map { collections ->
                collections[collectionNum] ?: false
            }
    }
}
