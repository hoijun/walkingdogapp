package com.example.walkingdogapp.datamodel

import android.os.Parcelable
import java.io.Serializable

@kotlinx.parcelize.Parcelize
data class CollectionInfo(
    val collectionNum: String = "000",
    val collectionName: String = "",
    val collectionText: String = "",
    val collectionResId: Int = 0
) : Serializable, Parcelable