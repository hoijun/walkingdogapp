package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing an alarm
 */
data class Alarm(
    val alarmCode: Int,
    val time: Long,
    val weeks: List<Boolean>,
    val isEnabled: Boolean
)
