package com.tulmunchi.walkingdogapp.utils

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEvent(events:List<Pair<String, String>>) {
        firebaseAnalytics.logEvent("tulmunchi_event") {
            events.forEach { (key, value) ->
                param(key, value)
            }
        }
    }
}