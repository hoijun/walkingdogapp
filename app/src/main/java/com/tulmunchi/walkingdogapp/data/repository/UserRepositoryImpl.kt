package com.tulmunchi.walkingdogapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.mapper.UserMapper
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSource
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Implementation of UserRepository
 */
class UserRepositoryImpl @Inject constructor(
    private val firebaseUserDataSource: FirebaseUserDataSource,
    private val auth: FirebaseAuth,
    private val networkChecker: NetworkChecker
) : UserRepository {

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override suspend fun getUser(): Result<User> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseUserDataSource.getUser(uid)
            .map { UserMapper.toDomain(it) }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        val userDto = UserMapper.toDto(user)
        return firebaseUserDataSource.saveUser(uid, userDto)
    }

    override suspend fun signUp(email: String): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseUserDataSource.signUp(uid, email)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(DomainError.NetworkError())
        }
        return firebaseUserDataSource.deleteUser(uid)
    }
}
