package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain model representing a collection item
 */
data class Collection(
    val collectionNum: String,
    val collectionName: String,
    val collectionText: String,
    val collectionResId: Int
)
