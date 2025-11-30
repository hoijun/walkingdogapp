package com.tulmunchi.walkingdogapp.domain.usecase.alarm

import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Use case for getting all alarms
 */
class GetAllAlarmsUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(): Result<List<Alarm>> {
        return alarmRepository.getAllAlarms()
    }
}
