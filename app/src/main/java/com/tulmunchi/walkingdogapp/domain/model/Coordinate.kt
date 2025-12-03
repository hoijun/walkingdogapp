package com.tulmunchi.walkingdogapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a geographic coordinate
 */
@Parcelize
data class Coordinate(
    val latitude: Double,
    val longitude: Double
) : Parcelable
