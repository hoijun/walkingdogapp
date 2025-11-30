package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.User

/**
 * Repository interface for user-related operations
 */
interface UserRepository {
    /**
     * Get user information
     */
    suspend fun getUser(): Result<User>

    /**
     * Update user information
     */
    suspend fun updateUser(user: User): Result<Unit>

    /**
     * Sign up a new user with email
     */
    suspend fun signUp(email: String): Result<Unit>

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit>
}
