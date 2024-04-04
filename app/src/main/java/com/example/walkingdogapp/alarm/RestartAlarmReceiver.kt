package com.example.walkingdogapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.walkingdogapp.userinfo.LocalUserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RestartAlarmReceiver: BroadcastReceiver() {
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private lateinit var functions: AlarmFunctions
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            functions = AlarmFunctions(context)
            coroutineScope.launch {
                val db = LocalUserDatabase.getInstance(context)
                val alarmDao = db!!.alarmDao()
                val alarms = alarmDao.getAlarmsList()
                alarms.let {
                    for (i in alarms.indices) {
                        val time = alarms[i].time
                        val code = alarms[i].alarm_code
                        val weeks = alarms[i].weeks
                        val alarmOn = alarms[i].alarmOn
                        functions.callAlarm(time, code, weeks, alarmOn)
                    }
                }
            }
        }
    }
}