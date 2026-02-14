package com.tulmunchi.walkingdogapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.model.UserDto
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSource
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    @Test
    fun `network가 없으면 getUser는 NetworkError를 반환한다`() = runBlocking {
        val dataSource = FakeFirebaseUserDataSource()
        val repository = UserRepositoryImpl(
            firebaseUserDataSource = dataSource,
            auth = mockAuth("uid-1"),
            networkChecker = FakeNetworkChecker(false)
        )

        val result = repository.getUser()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NetworkError)
        assertEquals(0, dataSource.getUserCallCount)
    }

    @Test
    fun `getUser 성공 시 UserDto를 User로 매핑한다`() = runBlocking {
        val dataSource = FakeFirebaseUserDataSource().apply {
            getUserResult = Result.success(
                UserDto(
                    email = "test@example.com",
                    name = "tester",
                    gender = "남",
                    birth = "2000/1/1"
                )
            )
        }
        val repository = UserRepositoryImpl(
            firebaseUserDataSource = dataSource,
            auth = mockAuth("uid-123"),
            networkChecker = FakeNetworkChecker(true)
        )

        val result = repository.getUser()

        assertTrue(result.isSuccess)
        assertEquals(
            User(
                email = "test@example.com",
                name = "tester",
                gender = "남",
                birth = "2000/1/1"
            ),
            result.getOrThrow()
        )
        assertEquals("uid-123", dataSource.lastUid)
    }

    @Test
    fun `updateUser는 매핑된 DTO와 uid를 전달한다`() = runBlocking {
        val dataSource = FakeFirebaseUserDataSource().apply {
            saveUserResult = Result.success(Unit)
        }
        val repository = UserRepositoryImpl(
            firebaseUserDataSource = dataSource,
            auth = mockAuth("uid-999"),
            networkChecker = FakeNetworkChecker(true)
        )
        val user = User(
            email = "a@b.com",
            name = "name",
            gender = "여",
            birth = "1999/1/1"
        )

        val result = repository.updateUser(user)

        assertTrue(result.isSuccess)
        assertEquals("uid-999", dataSource.savedUid)
        assertEquals(
            UserDto(
                email = "a@b.com",
                name = "name",
                gender = "여",
                birth = "1999/1/1"
            ),
            dataSource.savedUserDto
        )
    }

    private fun mockAuth(uid: String): FirebaseAuth {
        val auth = mock<FirebaseAuth>()
        val user = mock<FirebaseUser>()
        whenever(auth.currentUser).thenReturn(user)
        whenever(user.uid).thenReturn(uid)
        return auth
    }

    private class FakeNetworkChecker(
        private val available: Boolean
    ) : NetworkChecker {
        override fun isNetworkAvailable(): Boolean = available
    }

    private class FakeFirebaseUserDataSource : FirebaseUserDataSource {
        var getUserResult: Result<UserDto> = Result.success(UserDto())
        var saveUserResult: Result<Unit> = Result.success(Unit)
        var deleteUserResult: Result<Unit> = Result.success(Unit)
        var signUpResult: Result<Unit> = Result.success(Unit)
        var statsResult = Result.success(com.tulmunchi.walkingdogapp.data.model.WalkStatsDto())

        var getUserCallCount = 0
        var lastUid: String? = null
        var savedUid: String? = null
        var savedUserDto: UserDto? = null

        override suspend fun getUser(uid: String): Result<UserDto> {
            getUserCallCount++
            lastUid = uid
            return getUserResult
        }

        override suspend fun saveUser(uid: String, user: UserDto): Result<Unit> {
            savedUid = uid
            savedUserDto = user
            return saveUserResult
        }

        override suspend fun deleteUser(uid: String): Result<Unit> = deleteUserResult

        override suspend fun signUp(uid: String, email: String): Result<Unit> = signUpResult

        override suspend fun getTotalWalkStats(uid: String) = statsResult
    }
}
