package com.tulmunchi.walkingdogapp.presentation.ui.albummap

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.databinding.ItemAlbumMapPictureListBinding
import com.tulmunchi.walkingdogapp.presentation.model.AlbumMapImgInfo

class AlbumMapItemListAdapter(private val imgInfoList: List<AlbumMapImgInfo>): RecyclerView.Adapter<AlbumMapItemListAdapter.AlbumMapItemListViewHolder>() {
    private lateinit var context: Context

    fun interface OnItemClickListener {
        fun onItemClick(latLng: LatLng, tag: Int)
    }

    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumMapItemListViewHolder {
        val binding = ItemAlbumMapPictureListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return AlbumMapItemListViewHolder(binding)
    }

    override fun getItemCount(): Int = imgInfoList.size

    override fun onBindViewHolder(holder: AlbumMapItemListViewHolder, position: Int) {
        holder.bind(imgInfoList[position].uri)
    }

    inner class AlbumMapItemListViewHolder(private val binding: ItemAlbumMapPictureListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(imgInfoList[bindingAdapterPosition].latLng, imgInfoList[bindingAdapterPosition].tag)
            }
        }
        fun bind(img: Uri) {
            Glide.with(context).load(img).format(DecodeFormat.PREFER_ARGB_8888).override(400, 400).into(binding.albumImg)
        }
    }
}