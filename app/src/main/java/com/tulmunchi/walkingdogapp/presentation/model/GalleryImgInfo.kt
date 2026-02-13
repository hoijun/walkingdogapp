package com.tulmunchi.walkingdogapp.presentation.model

import android.net.Uri

data class GalleryImgInfo(
    var uri: Uri = Uri.EMPTY,
    var date: String = "",
    var width: Int = 0,
    var height: Int = 0
)