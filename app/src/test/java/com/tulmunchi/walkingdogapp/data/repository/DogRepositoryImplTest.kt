package com.tulmunchi.walkingdogapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.model.DogDto
import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import com.tulmunchi.walkingdogapp.data.model.WalkStatsDto
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseDogDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseStorageDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSource
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DogRepositoryImplTest {

    @Test
    fun `network가 없으면 getAllDogs는 NetworkError를 반환한다`() = runBlocking {
        val dogDataSource = FakeFirebaseDogDataSource()
        val repository = DogRepositoryImpl(
            firebaseDogDataSource = dogDataSource,
            firebaseStorageDataSource = FakeFirebaseStorageDataSource(),
            firebaseWalkDataSource = FakeFirebaseWalkDataSource(),
            auth = mockAuth("uid-1"),
            networkChecker = FakeNetworkChecker(false)
        )

        val result = repository.getAllDogs()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NetworkError)
        assertEquals(0, dogDataSource.getAllDogsCallCount)
    }

    @Test
    fun `getAllDogs 성공 시 DTO를 Domain으로 매핑한다`() = runBlocking {
        val dogDataSource = FakeFirebaseDogDataSource().apply {
            getAllDogsResult = Result.success(
                listOf(
                    DogDto(
                        name = "Mongi",
                        breed = "Poodle",
                        gender = "남",
                        birth = "2020/1/1",
                        neutering = "예",
                        vaccination = "예",
                        weight = "5",
                        feature = "friendly",
                        creationTime = 1L,
                        totalWalkInfo = WalkStatsDto(distance = 1500f, time = 3600)
                    )
                )
            )
        }
        val repository = DogRepositoryImpl(
            firebaseDogDataSource = dogDataSource,
            firebaseStorageDataSource = FakeFirebaseStorageDataSource(),
            firebaseWalkDataSource = FakeFirebaseWalkDataSource(),
            auth = mockAuth("uid-123"),
            networkChecker = FakeNetworkChecker(true)
        )

        val result = repository.getAllDogs()

        assertTrue(result.isSuccess)
        val dogs = result.getOrThrow()
        assertEquals(1, dogs.size)
        assertEquals("Mongi", dogs.first().name)
        assertEquals("Poodle", dogs.first().breed)
        assertEquals(1500f, dogs.first().dogWithStats.distance)
        assertEquals("uid-123", dogDataSource.lastUid)
    }

    @Test
    fun `getAllDogImages는 강아지 이름 목록으로 스토리지 조회를 수행한다`() = runBlocking {
        val dogDataSource = FakeFirebaseDogDataSource().apply {
            getAllDogsResult = Result.success(
                listOf(
                    DogDto(name = "Mongi"),
                    DogDto(name = "Bori")
                )
            )
        }
        val storageDataSource = FakeFirebaseStorageDataSource().apply {
            allDogImageUrlsResult = Result.success(
                mapOf(
                    "Mongi" to "https://img/mongi.jpg",
                    "Bori" to "https://img/bori.jpg"
                )
            )
        }
        val repository = DogRepositoryImpl(
            firebaseDogDataSource = dogDataSource,
            firebaseStorageDataSource = storageDataSource,
            firebaseWalkDataSource = FakeFirebaseWalkDataSource(),
            auth = mockAuth("uid-img"),
            networkChecker = FakeNetworkChecker(true)
        )

        val result = repository.getAllDogImages()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals(listOf("Mongi", "Bori"), storageDataSource.lastDogNames)
        assertEquals("uid-img", storageDataSource.lastUid)
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

    private class FakeFirebaseDogDataSource : FirebaseDogDataSource {
        var getAllDogsResult: Result<List<DogDto>> = Result.success(emptyList())
        var getDogResult: Result<DogDto> = Result.success(DogDto())
        var addDogResult: Result<Unit> = Result.success(Unit)
        var updateDogResult: Result<Unit> = Result.success(Unit)
        var deleteDogResult: Result<Unit> = Result.success(Unit)

        var getAllDogsCallCount = 0
        var lastUid: String? = null

        override suspend fun getAllDogs(uid: String): Result<List<DogDto>> {
            getAllDogsCallCount++
            lastUid = uid
            return getAllDogsResult
        }

        override suspend fun getDog(uid: String, dogName: String): Result<DogDto> = getDogResult

        override suspend fun addDog(uid: String, dog: DogDto): Result<Unit> = addDogResult

        override suspend fun updateDog(uid: String, oldName: String, dog: DogDto): Result<Unit> = updateDogResult

        override suspend fun deleteDog(uid: String, dogName: String): Result<Unit> = deleteDogResult
    }

    private class FakeFirebaseStorageDataSource : FirebaseStorageDataSource {
        var allDogImageUrlsResult: Result<Map<String, String>> = Result.success(emptyMap())
        var lastUid: String? = null
        var lastDogNames: List<String>? = null

        override suspend fun uploadDogImage(uid: String, dogName: String, imageUri: Uri): Result<String> {
            return Result.success("")
        }

        override suspend fun getDogImageUrl(uid: String, dogName: String): Result<String> = Result.success("")

        override suspend fun getAllDogImageUrls(uid: String, dogNames: List<String>): Result<Map<String, String>> {
            lastUid = uid
            lastDogNames = dogNames
            return allDogImageUrlsResult
        }

        override suspend fun deleteDogImage(uid: String, dogName: String): Result<Unit> = Result.success(Unit)

        override suspend fun copyDogImage(uid: String, oldName: String, newName: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeFirebaseWalkDataSource : FirebaseWalkDataSource {
        override suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun saveWalkRecords(uid: String, dogName: String, records: List<WalkRecordDto>): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>> {
            return Result.success(emptyMap())
        }
    }
}
