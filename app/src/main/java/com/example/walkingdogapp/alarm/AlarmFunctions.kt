package com.example.walkingdogapp.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.walkingdogapp.registerinfo.DoglistDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class AlarmFunctions(private val context: Context?) {
    private lateinit var pendingIntent: PendingIntent

    @SuppressLint("SimpleDateFormat", "ScheduleExactAlarm")
    fun callAlarm(time: Long, alarm_code: Int, weeks: Array<Boolean>?, onoff: Boolean) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val receiverIntent = Intent(context, AlarmReceiver::class.java) //리시버로 전달될 인텐트 설정


        receiverIntent.apply {
            putExtra("alarm_rqCode", alarm_code) //요청 코드를 리시버에 전달
            putExtra("week", weeks)
            putExtra("OnOff", onoff)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                alarm_code,
                receiverIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                alarm_code,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val alarmClock = AlarmManager.AlarmClockInfo(time, pendingIntent)
        alarmManager.setAlarmClock(alarmClock, pendingIntent)
    }

    fun cancelAlarm(alarm_code: Int) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(context, alarm_code, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getBroadcast(
                context,
                alarm_code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        alarmManager.cancel(pendingIntent)
    }
}