package com.tulmunchi.walkingdogapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing an alarm
 */
@Parcelize
data class Alarm(
    val alarmCode: Int,
    val time: Long,
    val weeks: List<Boolean>,
    val isEnabled: Boolean
) : Parcelable
