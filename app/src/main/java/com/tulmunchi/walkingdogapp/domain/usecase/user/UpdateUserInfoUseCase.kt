package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for updating user information
 */
class UpdateUserInfoUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        // Validate user data
        if (user.name.isBlank()) {
            return Result.failure(DomainError.ValidationError("이름을 입력해주세요"))
        }

        return userRepository.updateUser(user)
    }
}
