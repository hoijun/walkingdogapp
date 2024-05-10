package com.example.walkingdogapp.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import com.example.walkingdogapp.R
import com.example.walkingdogapp.SplashActivity
import com.example.walkingdogapp.repository.UserInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver: BroadcastReceiver() {

    private lateinit var manager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

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

        manager = context?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "This channel is used by Walking_Alarm"
            manager.createNotificationChannel(notificationChannel)
        }

        builder = NotificationCompat.Builder(context, CHANNEL_ID)

        val requestCode = intent?.extras!!.getInt("alarm_rqCode")

        val resultIntent = Intent(context, SplashActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                requestCode,
                resultIntent,
                PendingIntent.FLAG_IMMUTABLE
            ); //Activity를 시작하는 인텐트 생성
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle("털뭉치")
            .setDefaults(NotificationCompat.DEFAULT_ALL).setContentText("산책 할 시간 이에요!")
            .setContentIntent(pendingIntent).setAutoCancel(true).setOngoing(false).setPriority(
                PRIORITY_DEFAULT
            ).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()

        manager.notify(1, notification)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DATE, 1)
            set(Calendar.SECOND, 0)
        }

        val repository = UserInfoRepository(context.applicationContext as Application)
        val alarmFunctions = AlarmFunctions(context)
        alarmFunctions.cancelAlarm(requestCode)
        alarmFunctions.callAlarm(calendar.timeInMillis, requestCode, weeks)
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateAlarmTime(requestCode, calendar.timeInMillis)
        }
    }
}