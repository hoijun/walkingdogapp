package com.tulmunchi.walkingdogapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a walk record
 */
@Parcelize
data class WalkRecord(
    val day: String,
    val startTime: String,
    val endTime: String,
    val distance: Float,
    val time: Int,
    val coords: List<Coordinate>,
    val collections: List<String>
) : Parcelable
