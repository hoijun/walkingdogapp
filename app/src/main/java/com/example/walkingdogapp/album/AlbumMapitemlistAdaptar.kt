package com.example.walkingdogapp.album

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.AlbummapItemBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.InfoWindow

class AlbumMapitemlistAdaptar(private val imginfoList: MutableList<AlbumMapImgInfo>, private val context: Context) : RecyclerView.Adapter<AlbumMapitemlistAdaptar.AlbumMapitemlistViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(latLng: LatLng, tag: Int)
    }

    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumMapitemlistViewHolder {
        val binding = AlbummapItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumMapitemlistViewHolder(binding)
    }

    override fun getItemCount(): Int = imginfoList.size

    override fun onBindViewHolder(holder: AlbumMapitemlistViewHolder, position: Int) {
        holder.bind(imginfoList[position].uri)
    }

    inner class AlbumMapitemlistViewHolder(private val binding: AlbummapItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(imginfoList[bindingAdapterPosition].latLng, imginfoList[bindingAdapterPosition].tag)
            }
        }
        fun bind(img: Uri) {
            Glide.with(context).load(img).format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(binding.albumImg)
        }
    }
}