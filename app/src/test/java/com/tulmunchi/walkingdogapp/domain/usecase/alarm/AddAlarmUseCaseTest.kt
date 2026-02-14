package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddAlarmUseCaseTest {

    @Test
    fun `time이 0 이하면 ValidationError를 반환하고 저장하지 않는다`() = runBlocking {
        val repository = FakeAlarmRepository()
        val useCase = AddAlarmUseCase(repository)

        val result = useCase(
            Alarm(
                alarmCode = 1,
                time = 0L,
                weeks = listOf(true, false, false, false, false, false, false),
                isEnabled = true
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.addCalled)
    }

    @Test
    fun `요일이 비어있거나 전부 false면 ValidationError를 반환하고 저장하지 않는다`() = runBlocking {
        val repository = FakeAlarmRepository()
        val useCase = AddAlarmUseCase(repository)

        val result = useCase(
            Alarm(
                alarmCode = 2,
                time = 1000L,
                weeks = listOf(false, false, false, false, false, false, false),
                isEnabled = true
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.addCalled)
    }

    @Test
    fun `유효한 알람이면 repository addAlarm을 호출한다`() = runBlocking {
        val repository = FakeAlarmRepository()
        val useCase = AddAlarmUseCase(repository)
        val alarm = Alarm(
            alarmCode = 3,
            time = 1234L,
            weeks = listOf(true, true, false, false, false, false, false),
            isEnabled = true
        )

        val result = useCase(alarm)

        assertTrue(result.isSuccess)
        assertTrue(repository.addCalled)
        assertEquals(alarm, repository.lastAdded)
    }

    private class FakeAlarmRepository : AlarmRepository {
        var addCalled = false
        var lastAdded: Alarm? = null

        override suspend fun getAllAlarms(): Result<List<Alarm>> = Result.success(emptyList())

        override suspend fun addAlarm(alarm: Alarm): Result<Unit> {
            addCalled = true
            lastAdded = alarm
            return Result.success(Unit)
        }

        override suspend fun deleteAlarm(alarmCode: Int): Result<Unit> = Result.success(Unit)

        override suspend fun toggleAlarm(alarmCode: Int, isEnabled: Boolean): Result<Unit> = Result.success(Unit)

        override suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit> = Result.success(Unit)
    }
}
