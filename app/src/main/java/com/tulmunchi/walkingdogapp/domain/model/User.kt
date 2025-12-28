package com.tulmunchi.walkingdogapp.domain.model

import java.io.Serializable

/**
 * Domain model representing a user
 */
data class User(
    val email: String,
    var name: String,
    val gender: String,
    val birth: String
) : Serializable
