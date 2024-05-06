package com.example.walkingdogapp.userinfo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AlarmDataModel::class], version = 1)
@TypeConverters(WeekListConverters::class)
abstract class LocalUserDatabase: RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        private var instance: LocalUserDatabase? = null

        @Synchronized
        fun getInstance(context: Context): LocalUserDatabase? {
            if (instance == null) {
                synchronized(LocalUserDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LocalUserDatabase::class.java,
                        "user-database"
                    ).build()
                }
            }
            return instance
        }
    }
}