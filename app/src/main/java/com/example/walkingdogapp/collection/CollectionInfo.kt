package com.example.walkingdogapp.collection

import java.io.Serializable

data class CollectionInfo(val collectionNum: String = "000", val collectionName: String = "", val collectionText: String = "", val collectionImg: Int = 0, val keywords: List<String> = listOf()) : Serializable