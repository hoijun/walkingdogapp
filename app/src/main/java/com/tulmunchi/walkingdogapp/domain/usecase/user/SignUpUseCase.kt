package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for signing up a new user
 */
class SignUpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(DomainError.ValidationError("이메일을 입력해주세요"))
        }

        return userRepository.signUp(email)
    }
}
