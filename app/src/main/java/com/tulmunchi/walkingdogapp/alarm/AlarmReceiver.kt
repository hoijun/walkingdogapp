package com.tulmunchi.walkingdogapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.UpdateAlarmTimeUseCase
import com.tulmunchi.walkingdogapp.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {
    @Inject
    lateinit var updateAlarmTimeUseCase: UpdateAlarmTimeUseCase

    companion object{
        const val CHANNEL_ID = "WalkingDogApp_Channel"
        const val CHANNEL_NAME = "Walking_Alarm"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val weeks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra("week", Array<Boolean>::class.java)
        } else
            intent?.getSerializableExtra("week") as Array<Boolean>

        if (weeks != null) {
            val calendar = Calendar.getInstance()
            if (!weeks.get(calendar.get(Calendar.DAY_OF_WEEK) - 1))
                return
        }

        val notificationManager = context?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        val requestCode = intent?.extras?.getInt("alarm_rqCode") ?: 0

        val resultIntent = Intent(context, SplashActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                requestCode,
                resultIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon)
            .setContentTitle("털뭉치")
            .setContentText("산책 할 시간 이에요!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "This channel is used by Walking_Alarm"
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(requestCode, builder.build())

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DATE, 1)
            set(Calendar.SECOND, 0)
        }

        val alarmFunctions = AlarmFunctions(context)
        alarmFunctions.cancelAlarm(requestCode)
        alarmFunctions.callAlarm(calendar.timeInMillis, requestCode, weeks)
        CoroutineScope(Dispatchers.IO).launch {
            updateAlarmTimeUseCase(requestCode, calendar.timeInMillis)
        }
    }
}