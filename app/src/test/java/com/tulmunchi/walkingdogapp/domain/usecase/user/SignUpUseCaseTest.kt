package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignUpUseCaseTest {

    @Test
    fun `email이 비어있으면 ValidationError를 반환하고 repository를 호출하지 않는다`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = SignUpUseCase(repository)

        val result = useCase("")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.signUpCalled)
    }

    @Test
    fun `email이 유효하면 repository signUp을 호출한다`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = SignUpUseCase(repository)

        val result = useCase("test@example.com")

        assertTrue(result.isSuccess)
        assertTrue(repository.signUpCalled)
        assertEquals("test@example.com", repository.lastEmail)
    }

    private class FakeUserRepository : UserRepository {
        var signUpCalled = false
        var lastEmail: String? = null

        override suspend fun getUser(): Result<User> = Result.success(User("", "", "", ""))

        override suspend fun updateUser(user: User): Result<Unit> = Result.success(Unit)

        override suspend fun signUp(email: String): Result<Unit> {
            signUpCalled = true
            lastEmail = email
            return Result.success(Unit)
        }

        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }
}
