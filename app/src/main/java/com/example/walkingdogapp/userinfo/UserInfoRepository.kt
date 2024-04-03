package com.example.walkingdogapp.userinfo

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.walkingdogapp.alarm.AlarmDao
import com.example.walkingdogapp.alarm.AlarmDataModel

class UserInfoRepository(application: Application) {
    private val alarmDao: AlarmDao
    private val alarmList: LiveData<List<AlarmDataModel>>

    init {
        val db = LocalUserDatabase.getInstance(application)
        alarmDao = db!!.alarmDao()
        alarmList = alarmDao.getAllAlarms()
    }

    fun add(alarm: AlarmDataModel) {
        alarmDao.addAlarm(alarm)
    }

    fun delete(alarm: AlarmDataModel) {
        alarmDao.deleteAlarm(alarm.alarm_code)
    }

    fun getAll(): LiveData<List<AlarmDataModel>> {
        return alarmList
    }

    fun onOffAlarm(alarm_code: Int, alarmOn: Boolean) {
        alarmDao.updateAlarmStatus(alarm_code, alarmOn)
    }

    fun updateAlarmTime(alarm_code: Int, time: Long) {
        alarmDao.updateAlarmTime(alarm_code, time)
    }
}