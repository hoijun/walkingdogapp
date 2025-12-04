package com.tulmunchi.walkingdogapp.presentation.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.GetAllAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RestartAlarmReceiver: BroadcastReceiver() {
    @Inject
    lateinit var getAllAlarmsUseCase: GetAllAlarmsUseCase

    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            val functions = AlarmFunctions(context)
            coroutineScope.launch {
                getAllAlarmsUseCase().onSuccess { alarms ->
                    for (alarm in alarms) {
                        if(alarm.isEnabled) {
                            functions.callAlarm(alarm.time, alarm.alarmCode, alarm.weeks)
                        }
                    }
                }
            }
        }
    }
}