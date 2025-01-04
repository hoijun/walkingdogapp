package com.tulmunchi.walkingdogapp.datamodel

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import com.google.gson.Gson
import java.io.Serializable

class WeekListConverters {
    @TypeConverter
    fun listToJson(value: Array<Boolean>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToList(value: String): Array<Boolean>? {
        return Gson().fromJson(value,Array<Boolean>::class.java)
    }
}

@Entity(tableName = "active_alarms")
data class AlarmDataModel(
    @PrimaryKey(autoGenerate = true)
    var alarm_code: Int, // 알람 요청코드
    var time: Long, // 시간
    var weeks: Array<Boolean>, // 요일
    var alarmOn: Boolean
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmDataModel

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
    fun getAllAlarms() : LiveData<List<AlarmDataModel>> // 라이브 데이터 자체가 비동기적으로 작동하여 별도의 코루틴을 사용할 필요 없음.

    @Query("select * from active_alarms")
    fun getAlarmsList() : List<AlarmDataModel> // 반면 일반적인 리스트를 반환 할때는 코루틴을 사용해 반환 받아야 함.

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 알람은 중복되지 않게 저장
    fun addAlarm(item: AlarmDataModel)

    @Query("DELETE FROM active_alarms WHERE alarm_code = :alarm_code") // 알람 코드로 삭제
    fun deleteAlarm(alarm_code: Int)

    @Query("UPDATE active_alarms SET alarmOn = :alarmOn WHERE alarm_code = :alarm_code")
    fun updateAlarmStatus(alarm_code: Int, alarmOn: Boolean)

    @Query("UPDATE active_alarms SET time = :time WHERE alarm_code = :alarm_code")
    fun updateAlarmTime(alarm_code: Int, time: Long)
}