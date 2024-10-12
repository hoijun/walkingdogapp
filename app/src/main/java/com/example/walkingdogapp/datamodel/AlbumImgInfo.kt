package com.example.walkingdogapp.datamodel

import android.net.Uri
import android.widget.ImageView
import com.naver.maps.geometry.LatLng

data class AlbumMapImgInfo(
    var uri: Uri = Uri.EMPTY,
    var latLng: LatLng = LatLng(0.0, 0.0),
    val imgView: ImageView? = null,
    var tag: Int = 0
)