package com.tulmunchi.walkingdogapp.datamodel

import android.net.Uri

data class GalleryImgInfo(
    var uri: Uri = Uri.EMPTY,
    var title: String = "",
    var date: String = "",
    var width: Int = 0,
    var height: Int = 0
)