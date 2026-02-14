package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateAlarmTimeUseCaseTest {

    @Test
    fun `time이 0 이하면 ValidationError를 반환하고 repository를 호출하지 않는다`() = runBlocking {
        val repository = FakeAlarmRepository()
        val useCase = UpdateAlarmTimeUseCase(repository)

        val result = useCase(alarmCode = 1, time = 0L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.updateCalled)
    }

    @Test
    fun `유효한 time이면 repository updateAlarmTime을 호출한다`() = runBlocking {
        val repository = FakeAlarmRepository()
        val useCase = UpdateAlarmTimeUseCase(repository)

        val result = useCase(alarmCode = 7, time = 12345L)

        assertTrue(result.isSuccess)
        assertTrue(repository.updateCalled)
        assertEquals(7, repository.lastAlarmCode)
        assertEquals(12345L, repository.lastTime)
    }

    private class FakeAlarmRepository : AlarmRepository {
        var updateCalled = false
        var lastAlarmCode: Int? = null
        var lastTime: Long? = null

        override suspend fun getAllAlarms(): Result<List<Alarm>> = Result.success(emptyList())

        override suspend fun addAlarm(alarm: Alarm): Result<Unit> = Result.success(Unit)

        override suspend fun deleteAlarm(alarmCode: Int): Result<Unit> = Result.success(Unit)

        override suspend fun toggleAlarm(alarmCode: Int, isEnabled: Boolean): Result<Unit> = Result.success(Unit)

        override suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit> {
            updateCalled = true
            lastAlarmCode = alarmCode
            lastTime = time
            return Result.success(Unit)
        }
    }
}
