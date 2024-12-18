package com.example.walkingdogapp.album

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.DetailpicturelistItemBinding
import com.example.walkingdogapp.datamodel.GalleryImgInfo

class DetailPictureItemListAdapter(private val imgList: List<GalleryImgInfo>) : RecyclerView.Adapter<DetailPictureItemListAdapter.DetailPictureItemListViewHolder>(){
    private lateinit var context: Context
    private var screenWidth = 0
    private var screenHeight = 0

    fun interface OnClickItemListener {
        fun onClickItem(imgInfo: GalleryImgInfo)
    }

    var onClickItemListener: OnClickItemListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailPictureItemListViewHolder {
        context = parent.context
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
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
            Glide.with(context).load(imgInfo.uri).override(screenWidth, screenHeight)
                .format(DecodeFormat.PREFER_ARGB_8888).into(binding.galleryImg)
        }
    }
}