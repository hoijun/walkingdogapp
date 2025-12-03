package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.database.FirebaseDatabase
import com.tulmunchi.walkingdogapp.data.model.UserDto
import com.tulmunchi.walkingdogapp.data.model.WalkStatsDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firebase DataSource for User operations
 */
interface FirebaseUserDataSource {
    suspend fun getUser(uid: String): Result<UserDto>
    suspend fun saveUser(uid: String, user: UserDto): Result<Unit>
    suspend fun deleteUser(uid: String): Result<Unit>
    suspend fun signUp(uid: String, email: String): Result<Unit>
    suspend fun getTotalWalkStats(uid: String): Result<WalkStatsDto>
}

class FirebaseUserDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : FirebaseUserDataSource {

    override suspend fun getUser(uid: String): Result<UserDto> {
        return try {
            val snapshot = database.getReference("Users").child(uid).child("user")
                .get()
                .await()
            val user = snapshot.getValue(UserDto::class.java) ?: UserDto()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUser(uid: String, user: UserDto): Result<Unit> {
        return try {
            database.getReference("Users").child(uid).child("user")
                .setValue(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            database.getReference("Users").child(uid)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(uid: String, email: String): Result<Unit> {
        return try {
            val userRef = database.getReference("Users").child(uid)

            // Create user node
            userRef.child("user").setValue(UserDto(email = email)).await()

            // Create total walk info node
            userRef.child("totalWalkInfo").setValue(WalkStatsDto()).await()

            // Create collection node (empty for now, will be populated later)
            userRef.child("collection").setValue(HashMap<String, Boolean>()).await()

            // Create terms of service acceptance
            userRef.child("termsOfService").setValue(true).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalWalkStats(uid: String): Result<WalkStatsDto> {
        return try {
            val snapshot = database.getReference("Users").child(uid).child("totalWalkInfo")
                .get()
                .await()
            val stats = snapshot.getValue(WalkStatsDto::class.java) ?: WalkStatsDto()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
