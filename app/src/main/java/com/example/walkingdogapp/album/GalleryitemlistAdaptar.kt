package com.example.walkingdogapp.album

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.GallerylistItemBinding
import com.naver.maps.geometry.LatLng

class GalleryitemlistAdaptar(private val imgList: MutableList<GalleryImgInfo>, private val context: Context) : RecyclerView.Adapter<GalleryitemlistAdaptar.AlbumMapitemlistViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(latLng: LatLng, tag: Int)
    }

    var itemClickListener : GalleryitemlistAdaptar.OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumMapitemlistViewHolder {
        val binding = GallerylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumMapitemlistViewHolder(binding)
    }

    override fun getItemCount(): Int = imgList.size

    override fun onBindViewHolder(holder: AlbumMapitemlistViewHolder, position: Int) {
        holder.bind(imgList[position])
    }

    inner class AlbumMapitemlistViewHolder(private val binding: GallerylistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
            }
        }
        fun bind(ImgInfo: GalleryImgInfo) {

        }
    }
}