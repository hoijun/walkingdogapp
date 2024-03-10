package com.example.walkingdogapp

import kotlin.collections.HashMap


class Constant {
    companion object {
        const val Walking_SERVICE_ID = 175
        const val ACTION_START_Walking_SERVICE = "startWalkingService"
        const val ACTION_STOP_Walking_SERVICE = "stopWalkingService"
        const val ACTION_START_Walking_Tracking = "startWalkingTracking"
        const val ACTION_STOP_Walking_Tracking = "stopWalkingTracking"

        val item_whether = HashMap<String, Boolean>().apply {
            for (num: Int in 1..11) {
                put(String.format("%03d", num), false)
            }
        }
    }
}