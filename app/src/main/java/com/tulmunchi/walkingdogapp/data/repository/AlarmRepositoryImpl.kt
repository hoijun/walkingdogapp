package com.tulmunchi.walkingdogapp.data.repository

import com.tulmunchi.walkingdogapp.data.mapper.AlarmMapper
import com.tulmunchi.walkingdogapp.data.source.local.AlarmLocalDataSource
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Implementation of AlarmRepository
 */
class AlarmRepositoryImpl @Inject constructor(
    private val alarmLocalDataSource: AlarmLocalDataSource
) : AlarmRepository {

    override suspend fun getAllAlarms(): Result<List<Alarm>> {
        return alarmLocalDataSource.getAllAlarms()
            .map { AlarmMapper.toDomainList(it) }
    }

    override suspend fun addAlarm(alarm: Alarm): Result<Unit> {
        val alarmEntity = AlarmMapper.toEntity(alarm)
        return alarmLocalDataSource.addAlarm(alarmEntity)
    }

    override suspend fun deleteAlarm(alarmCode: Int): Result<Unit> {
        return alarmLocalDataSource.deleteAlarm(alarmCode)
    }

    override suspend fun toggleAlarm(alarmCode: Int, isEnabled: Boolean): Result<Unit> {
        return alarmLocalDataSource.updateAlarmStatus(alarmCode, isEnabled)
    }

    override suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit> {
        return alarmLocalDataSource.updateAlarmTime(alarmCode, time)
    }
}
