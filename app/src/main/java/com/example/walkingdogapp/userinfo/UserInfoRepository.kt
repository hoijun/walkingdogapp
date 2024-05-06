package com.example.walkingdogapp.userinfo

import android.app.Application

class UserInfoRepository(application: Application) {
    private val alarmDao: AlarmDao
    private lateinit var alarmList: List<AlarmDataModel>

    init {
        val db = LocalUserDatabase.getInstance(application)
        alarmDao = db!!.alarmDao()
    }

    fun add(alarm: AlarmDataModel) {
        alarmDao.addAlarm(alarm)
    }

    fun delete(alarm: AlarmDataModel) {
        alarmDao.deleteAlarm(alarm.alarm_code)
    }

    fun getAll(): List<AlarmDataModel> {
        alarmList = alarmDao.getAlarmsList()
        return alarmList
    }

    fun onOffAlarm(alarm_code: Int, alarmOn: Boolean) {
        alarmDao.updateAlarmStatus(alarm_code, alarmOn)
    }

    fun updateAlarmTime(alarm_code: Int, time: Long) {
        alarmDao.updateAlarmTime(alarm_code, time)
    }
}