package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing a user
 */
data class User(
    val email: String,
    val name: String,
    val gender: String,
    val birth: String
)
