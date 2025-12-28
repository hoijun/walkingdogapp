package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for getting user information
 */
class GetUserInfoUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<User> {
        return userRepository.getUser()
    }
}
