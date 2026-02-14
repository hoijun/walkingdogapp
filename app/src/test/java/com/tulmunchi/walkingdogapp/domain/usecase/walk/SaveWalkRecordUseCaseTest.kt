package com.tulmunchi.walkingdogapp.domain.usecase.walk

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveWalkRecordUseCaseTest {

    @Test
    fun `dogNames가 비어있으면 ValidationError를 반환하고 repository를 호출하지 않는다`() = runBlocking {
        val repository = FakeWalkRepository()
        val useCase = SaveWalkRecordUseCase(repository)

        val result = useCase(
            dogNames = emptyList(),
            walkRecord = dummyWalkRecord()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.saveCalled)
    }

    @Test
    fun `dogNames가 있으면 repository saveWalkRecord를 호출한다`() = runBlocking {
        val repository = FakeWalkRepository()
        val useCase = SaveWalkRecordUseCase(repository)

        val result = useCase(
            dogNames = listOf("Mongi", "Bori"),
            walkRecord = dummyWalkRecord()
        )

        assertTrue(result.isSuccess)
        assertTrue(repository.saveCalled)
        assertEquals(listOf("Mongi", "Bori"), repository.lastDogNames)
    }

    private fun dummyWalkRecord() = WalkRecord(
        day = "2026-02-14",
        startTime = "10:00",
        endTime = "10:30",
        distance = 500f,
        time = 1800,
        calories = 100f,
        poopCoordinates = emptyList(),
        memoCoordinates = emptyMap(),
        walkCoordinates = emptyList(),
        collections = emptyList()
    )

    private class FakeWalkRepository : WalkRepository {
        var saveCalled = false
        var lastDogNames: List<String>? = null

        override suspend fun saveWalkRecord(
            dogNames: List<String>,
            walkRecord: WalkRecord
        ): Result<Unit> {
            saveCalled = true
            lastDogNames = dogNames
            return Result.success(Unit)
        }

        override suspend fun getAllWalkHistory(): Result<Map<String, List<WalkRecord>>> {
            return Result.success(emptyMap())
        }

        override suspend fun getTotalWalkStats(): Result<WalkStats> {
            return Result.success(WalkStats())
        }
    }
}
