package com.example.walkingdogapp.album

import android.net.Uri
import android.widget.ImageView
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.InfoWindow

data class AlbumMapImgInfo(var uri: Uri = Uri.EMPTY, var latLng: LatLng = LatLng(0.0, 0.0), val imgView: ImageView? = null, var tag: Int = 0)
data class GalleryImgInfo(var uri: Uri = Uri.EMPTY, var title: String = "", var date: String = "")