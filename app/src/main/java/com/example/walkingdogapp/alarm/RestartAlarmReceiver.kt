package com.example.walkingdogapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.walkingdogapp.userinfo.LocalUserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RestartAlarmReceiver: BroadcastReceiver() {
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private lateinit var functions: AlarmFunctions
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("savaepoint", "aaaaa")
        if(intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            functions = AlarmFunctions(context)
            coroutineScope.launch {
                val db = LocalUserDatabase.getInstance(context)
                val list = db!!.alarmDao().getAllAlarms().value?: listOf()
                val size = list.size
                Log.d("savaepoint", size.toString())
                list.let {
                    for(i in 0 until size) {
                        val time = list[i].time
                        val code = list[i].alarm_code
                        val weeks = list[i].weeks
                        val alarmOn = list[i].alarmOn
                        functions.callAlarm(time, code, weeks, alarmOn)
                    }
                }
            }
        }
    }
}