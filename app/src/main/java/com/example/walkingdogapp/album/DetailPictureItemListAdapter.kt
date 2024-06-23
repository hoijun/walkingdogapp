package com.example.walkingdogapp.album

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.target.Target
import com.example.walkingdogapp.databinding.DetailpicturelistItemBinding

class DetailPictureItemListAdapter(private val imgList: List<GalleryImgInfo>, private val context: Context) : RecyclerView.Adapter<DetailPictureItemListAdapter.DetailPictureItemListViewHolder>(){

    fun interface OnClickItemListener {
        fun onClickItem(imgInfo: GalleryImgInfo)
    }

    var onClickItemListener: OnClickItemListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailPictureItemListViewHolder {
        val binding = DetailpicturelistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailPictureItemListViewHolder(binding)
    }

    override fun getItemCount(): Int = imgList.size

    override fun onBindViewHolder(holder: DetailPictureItemListViewHolder, position: Int) {
        holder.bind(imgList[position])
    }

    inner class DetailPictureItemListViewHolder(private val binding: DetailpicturelistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onClickItemListener?.onClickItem(imgList[bindingAdapterPosition])
            }
        }

        fun bind(imgInfo: GalleryImgInfo) {
            Glide.with(context).load(imgInfo.uri).override(Target.SIZE_ORIGINAL)
                .format(DecodeFormat.PREFER_ARGB_8888).into(binding.galleryImg)
        }
    }
}