package com.tulmunchi.walkingdogapp.data.source.local.entity

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import com.google.gson.Gson

class WeekListConverters {
    @TypeConverter
    fun listToJson(value: Array<Boolean>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToList(value: String): Array<Boolean>? {
        return Gson().fromJson(value, Array<Boolean>::class.java)
    }
}

@Entity(tableName = "active_alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    var alarm_code: Int,
    var time: Long,
    var weeks: Array<Boolean>,
    var alarmOn: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmEntity

        if (alarm_code != other.alarm_code) return false
        if (time != other.time) return false
        if (!weeks.contentEquals(other.weeks)) return false
        if (alarmOn != other.alarmOn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alarm_code
        result = 31 * result + time.hashCode()
        result = 31 * result + weeks.contentHashCode()
        result = 31 * result + alarmOn.hashCode()
        return result
    }
}


@Dao
interface AlarmDao {
    @Query("select * from active_alarms")
    fun getAllAlarms(): LiveData<List<AlarmEntity>>

    @Query("select * from active_alarms")
    fun getAlarmsList(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAlarm(item: AlarmEntity)

    @Query("DELETE FROM active_alarms WHERE alarm_code = :alarm_code")
    fun deleteAlarm(alarm_code: Int)

    @Query("UPDATE active_alarms SET alarmOn = :alarmOn WHERE alarm_code = :alarm_code")
    fun updateAlarmStatus(alarm_code: Int, alarmOn: Boolean)

    @Query("UPDATE active_alarms SET time = :time WHERE alarm_code = :alarm_code")
    fun updateAlarmTime(alarm_code: Int, time: Long)
}
