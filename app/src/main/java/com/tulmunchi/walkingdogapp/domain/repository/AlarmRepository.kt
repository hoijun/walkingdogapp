package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.Alarm

/**
 * Repository interface for alarm-related operations
 */
interface AlarmRepository {
    /**
     * Get all alarms
     */
    suspend fun getAllAlarms(): Result<List<Alarm>>

    /**
     * Add a new alarm
     */
    suspend fun addAlarm(alarm: Alarm): Result<Unit>

    /**
     * Delete an alarm
     */
    suspend fun deleteAlarm(alarmCode: Int): Result<Unit>

    /**
     * Toggle alarm on/off
     */
    suspend fun toggleAlarm(alarmCode: Int, isEnabled: Boolean): Result<Unit>

    /**
     * Update alarm time
     */
    suspend fun updateAlarmTime(alarmCode: Int, time: Long): Result<Unit>
}
