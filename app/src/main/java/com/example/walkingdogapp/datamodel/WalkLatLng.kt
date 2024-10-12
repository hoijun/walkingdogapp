package com.example.walkingdogapp.datamodel

import java.io.Serializable

data class WalkLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable