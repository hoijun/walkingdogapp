package com.example.walkingdogapp.datamodel

import android.net.Uri

data class GalleryImgInfo(
    var uri: Uri = Uri.EMPTY,
    var title: String = "",
    var date: String = ""
)