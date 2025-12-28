package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for deleting user account
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.deleteAccount()
    }
}
