package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing all initial data loaded at app start
 */
data class InitialData(
    val user: User?,
    val dogs: List<Dog>,
    val dogImages: Map<String, String>, // Map of dog name to image URI string
    val totalWalkStats: WalkStats,
    val walkHistory: Map<String, List<WalkRecord>>,
    val collections: Map<String, Boolean>,
    val alarms: List<Alarm>
)
