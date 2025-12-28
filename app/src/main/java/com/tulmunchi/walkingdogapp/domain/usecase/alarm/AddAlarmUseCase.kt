package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Use case for adding a new alarm
 */
class AddAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarm: Alarm): Result<Unit> {
        // Validate alarm data
        if (alarm.time <= 0) {
            return Result.failure(DomainError.ValidationError("알람 시간이 올바르지 않습니다"))
        }

        if (alarm.weeks.isEmpty() || alarm.weeks.all { !it }) {
            return Result.failure(DomainError.ValidationError("알람 요일을 선택해주세요"))
        }

        return alarmRepository.addAlarm(alarm)
    }
}
