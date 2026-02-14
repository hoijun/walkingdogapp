package com.tulmunchi.walkingdogapp.domain.usecase.user

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateUserInfoUseCaseTest {

    @Test
    fun `name이 비어있으면 ValidationError를 반환하고 repository를 호출하지 않는다`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateUserInfoUseCase(repository)

        val result = useCase(
            User(
                email = "test@example.com",
                name = "",
                gender = "남",
                birth = "2000/1/1"
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.updateCalled)
    }

    @Test
    fun `유효한 user면 repository updateUser를 호출한다`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateUserInfoUseCase(repository)
        val user = User(
            email = "test@example.com",
            name = "tester",
            gender = "남",
            birth = "2000/1/1"
        )

        val result = useCase(user)

        assertTrue(result.isSuccess)
        assertTrue(repository.updateCalled)
        assertEquals(user, repository.lastUser)
    }

    private class FakeUserRepository : UserRepository {
        var updateCalled = false
        var lastUser: User? = null

        override suspend fun getUser(): Result<User> = Result.success(User("", "", "", ""))

        override suspend fun updateUser(user: User): Result<Unit> {
            updateCalled = true
            lastUser = user
            return Result.success(Unit)
        }

        override suspend fun signUp(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }
}
