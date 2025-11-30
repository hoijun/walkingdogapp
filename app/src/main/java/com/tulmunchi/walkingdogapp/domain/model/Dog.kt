package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing a dog
 */
data class Dog(
    val name: String,
    val breed: String,
    val gender: String,
    val birth: String,
    val neutering: String,
    val vaccination: String,
    val weight: String,
    val feature: String,
    val creationTime: Long
)

/**
 * Domain model representing a dog with its walk statistics
 */
data class DogWithStats(
    val dog: Dog,
    val stats: WalkStats
)
