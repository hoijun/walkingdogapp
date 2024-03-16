package com.example.walkingdogapp.album

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.GallerylistItemBinding
import com.naver.maps.geometry.LatLng

class GalleryitemlistAdaptar(private val imgList: MutableList<GalleryImgInfo>, private val context: Context) : RecyclerView.Adapter<GalleryitemlistAdaptar.AlbumMapitemlistViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(imgInfo: GalleryImgInfo)
    }

    var itemClickListener : OnItemClickListener? = null

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
                itemClickListener?.onItemClick(imgList[bindingAdapterPosition])
            }
        }
        fun bind(imgInfo: GalleryImgInfo) {
            Glide.with(context).load(imgInfo.uri).format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(binding.galleryImg)
        }
    }
}