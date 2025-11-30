package com.tulmunchi.walkingdogapp.data.source.local

import com.tulmunchi.walkingdogapp.datamodel.AlarmDao
import com.tulmunchi.walkingdogapp.datamodel.AlarmDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Local DataSource for Alarm operations (Room)
 */
interface AlarmLocalDataSource {
    suspend fun getAllAlarms(): Result<List<AlarmDataModel>>
    suspend fun addAlarm(alarm: AlarmDataModel): Result<Unit>
    suspend fun deleteAlarm(alarmCode: Int): Result<Unit>
    suspend fun updateAlarmStatus(alarmCode: Int, isEnabled: Boolean): Result<Unit>
    suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit>
}

class AlarmLocalDataSourceImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmLocalDataSource {

    override suspend fun getAllAlarms(): Result<List<AlarmDataModel>> {
        return try {
            val alarms = withContext(Dispatchers.IO) {
                alarmDao.getAlarmsList()
            }
            Result.success(alarms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addAlarm(alarm: AlarmDataModel): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                alarmDao.addAlarm(alarm)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAlarm(alarmCode: Int): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                alarmDao.deleteAlarm(alarmCode)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAlarmStatus(alarmCode: Int, isEnabled: Boolean): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                alarmDao.updateAlarmStatus(alarmCode, isEnabled)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                alarmDao.updateAlarmTime(alarmCode, time)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
