package com.example.walkingdogapp.datamodel

data class WalkDateInfoInSave (
    val distance: Float = 0.0f,
    val time: Int = 0,
    val coords: List<WalkLatLng> = listOf(),
    val collections: List<String>
) // 산책 한 후 저장 할 때 쓰는 클래스
