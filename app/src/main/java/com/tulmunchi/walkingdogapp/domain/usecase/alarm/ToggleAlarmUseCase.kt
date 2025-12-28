package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Use case for toggling alarm on/off
 */
class ToggleAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarmCode: Int, isEnabled: Boolean): Result<Unit> {
        return alarmRepository.toggleAlarm(alarmCode, isEnabled)
    }
}
