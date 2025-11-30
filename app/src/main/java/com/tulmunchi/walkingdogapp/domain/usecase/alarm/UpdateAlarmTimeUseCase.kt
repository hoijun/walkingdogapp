package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Use case for updating alarm time
 */
class UpdateAlarmTimeUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarmCode: Int, time: Long): Result<Unit> {
        if (time <= 0) {
            return Result.failure(DomainError.ValidationError("알람 시간이 올바르지 않습니다"))
        }

        return alarmRepository.updateAlarmTime(alarmCode, time)
    }
}
