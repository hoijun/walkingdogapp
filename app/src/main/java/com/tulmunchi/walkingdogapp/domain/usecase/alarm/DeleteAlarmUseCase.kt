package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Use case for deleting an alarm
 */
class DeleteAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarmCode: Int): Result<Unit> {
        return alarmRepository.deleteAlarm(alarmCode)
    }
}
