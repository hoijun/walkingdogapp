package com.tulmunchi.walkingdogapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.data.model.CoordinateDto
import com.tulmunchi.walkingdogapp.data.model.UserDto
import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import com.tulmunchi.walkingdogapp.data.model.WalkStatsDto
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseCollectionDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseUserDataSource
import com.tulmunchi.walkingdogapp.data.source.remote.FirebaseWalkDataSource
import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalkRepositoryImplTest {

    @Test
    fun `network가 없으면 saveWalkRecord는 NetworkError를 반환한다`() = runBlocking {
        val walkDataSource = FakeFirebaseWalkDataSource()
        val collectionDataSource = FakeFirebaseCollectionDataSource()
        val repository = WalkRepositoryImpl(
            firebaseWalkDataSource = walkDataSource,
            firebaseUserDataSource = FakeFirebaseUserDataSource(),
            firebaseCollectionDataSource = collectionDataSource,
            auth = mockAuth("uid-1"),
            networkChecker = FakeNetworkChecker(false)
        )

        val result = repository.saveWalkRecord(
            dogNames = listOf("Mongi"),
            walkRecord = sampleWalkRecord()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NetworkError)
        assertEquals(0, walkDataSource.saveWalkRecordCalls.size)
        assertEquals(0, collectionDataSource.updateCollectionCallCount)
    }

    @Test
    fun `saveWalkRecord 성공 시 dog별 저장과 collection 업데이트를 수행한다`() = runBlocking {
        val walkDataSource = FakeFirebaseWalkDataSource()
        val collectionDataSource = FakeFirebaseCollectionDataSource()
        val repository = WalkRepositoryImpl(
            firebaseWalkDataSource = walkDataSource,
            firebaseUserDataSource = FakeFirebaseUserDataSource(),
            firebaseCollectionDataSource = collectionDataSource,
            auth = mockAuth("uid-abc"),
            networkChecker = FakeNetworkChecker(true)
        )

        val result = repository.saveWalkRecord(
            dogNames = listOf("Mongi", "Bori"),
            walkRecord = sampleWalkRecord()
        )

        assertTrue(result.isSuccess)
        assertEquals(2, walkDataSource.saveWalkRecordCalls.size)
        assertEquals("uid-abc", walkDataSource.saveWalkRecordCalls[0].uid)
        assertEquals("Mongi", walkDataSource.saveWalkRecordCalls[0].dogName)
        assertEquals("Bori", walkDataSource.saveWalkRecordCalls[1].dogName)
        assertEquals(1, collectionDataSource.updateCollectionCallCount)
        assertEquals(listOf("001", "002"), collectionDataSource.lastCollections)
    }

    @Test
    fun `getAllWalkHistory는 DTO를 Domain으로 매핑한다`() = runBlocking {
        val walkDataSource = FakeFirebaseWalkDataSource().apply {
            allWalkHistoryResult = Result.success(
                mapOf(
                    "Mongi" to listOf(
                        WalkRecordDto(
                            day = "2026-02-14",
                            startTime = "10:00",
                            endTime = "10:30",
                            distance = 300f,
                            time = 1800,
                            calories = 120f,
                            poopCoordinates = listOf(CoordinateDto(37.5, 127.0)),
                            memoCoordinates = mapOf("memo" to CoordinateDto(37.6, 127.1)),
                            walkCoordinates = listOf(CoordinateDto(37.7, 127.2)),
                            collections = listOf("001")
                        )
                    )
                )
            )
        }
        val repository = WalkRepositoryImpl(
            firebaseWalkDataSource = walkDataSource,
            firebaseUserDataSource = FakeFirebaseUserDataSource(),
            firebaseCollectionDataSource = FakeFirebaseCollectionDataSource(),
            auth = mockAuth("uid-his"),
            networkChecker = FakeNetworkChecker(true)
        )

        val result = repository.getAllWalkHistory()

        assertTrue(result.isSuccess)
        val mongiHistory = result.getOrThrow()["Mongi"].orEmpty()
        assertEquals(1, mongiHistory.size)
        assertEquals(300f, mongiHistory.first().distance)
        assertEquals(Coordinate(37.5, 127.0), mongiHistory.first().poopCoordinates.first())
        assertEquals(Coordinate(37.6, 127.1), mongiHistory.first().memoCoordinates["memo"])
    }

    private fun sampleWalkRecord(): WalkRecord {
        return WalkRecord(
            day = "2026-02-14",
            startTime = "10:00",
            endTime = "10:30",
            distance = 300f,
            time = 1800,
            calories = 120f,
            poopCoordinates = listOf(Coordinate(37.5, 127.0)),
            memoCoordinates = mapOf("memo" to Coordinate(37.6, 127.1)),
            walkCoordinates = listOf(Coordinate(37.7, 127.2)),
            collections = listOf("001", "002")
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

    private class FakeFirebaseWalkDataSource : FirebaseWalkDataSource {
        data class SaveCall(
            val uid: String,
            val dogName: String,
            val record: WalkRecordDto
        )

        var saveWalkRecordResult: Result<Unit> = Result.success(Unit)
        var allWalkHistoryResult: Result<Map<String, List<WalkRecordDto>>> = Result.success(emptyMap())
        val saveWalkRecordCalls = mutableListOf<SaveCall>()

        override suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit> {
            saveWalkRecordCalls.add(SaveCall(uid, dogName, record))
            return saveWalkRecordResult
        }

        override suspend fun saveWalkRecords(uid: String, dogName: String, records: List<WalkRecordDto>): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>> {
            return allWalkHistoryResult
        }
    }

    private class FakeFirebaseCollectionDataSource : FirebaseCollectionDataSource {
        var updateCollectionCallCount = 0
        var lastCollections: List<String>? = null

        override suspend fun getAllCollections(uid: String): Result<Map<String, Boolean>> {
            return Result.success(emptyMap())
        }

        override suspend fun updateCollection(uid: String, collection: List<String>): Result<Unit> {
            updateCollectionCallCount++
            lastCollections = collection
            return Result.success(Unit)
        }
    }

    private class FakeFirebaseUserDataSource : FirebaseUserDataSource {
        override suspend fun getUser(uid: String): Result<UserDto> = Result.success(UserDto())

        override suspend fun saveUser(uid: String, user: UserDto): Result<Unit> = Result.success(Unit)

        override suspend fun deleteUser(uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun signUp(uid: String, email: String): Result<Unit> = Result.success(Unit)

        override suspend fun getTotalWalkStats(uid: String): Result<WalkStatsDto> = Result.success(WalkStatsDto())
    }
}
