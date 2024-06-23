package com.example.walkingdogapp.album

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.GallerylistItemBinding

class GalleryItemListAdapter(private val imgList: MutableList<GalleryImgInfo>) : RecyclerView.Adapter<GalleryItemListAdapter.AlbumMapItemListViewHolder>() {
    private lateinit var context: Context
    interface OnItemClickListener {
        fun onItemClick(imgNum: Int)
        fun onItemLongClick(imgUri: Uri)
        fun onItemClickInSelectMode(imgUri: Uri)
    }

    private val selectedItems = mutableListOf<GalleryImgInfo>()
    private var selectMode = false
    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumMapItemListViewHolder {
        val binding = GallerylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return AlbumMapItemListViewHolder(binding)
    }

    override fun getItemCount(): Int = imgList.size

    override fun onBindViewHolder(holder: AlbumMapItemListViewHolder, position: Int) {
        holder.bind(imgList[position])
    }

    inner class AlbumMapItemListViewHolder(private val binding: GallerylistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (!selectMode) {
                    itemClickListener?.onItemClick(bindingAdapterPosition)
                } else {
                    itemClickListener?.onItemClickInSelectMode(imgList[bindingAdapterPosition].uri)
                    toggleSelection(imgList[bindingAdapterPosition])
                }
            }

            binding.root.setOnLongClickListener {
                itemClickListener?.onItemLongClick(imgList[bindingAdapterPosition].uri)
                toggleSelection(imgList[bindingAdapterPosition])
                selectMode = true

                true
            }
        }
        fun bind(imgInfo: GalleryImgInfo) {
            Glide.with(context).load(imgInfo.uri).format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(binding.galleryImg)
            if (selectMode) {
                binding.checkBox.visibility = View.VISIBLE
                binding.checkBox.isChecked = selectedItems.contains(imgList[bindingAdapterPosition])
            } else {
                binding.checkBox.visibility = View.GONE
            }
        }
    }

    private fun toggleSelection(imgInfo: GalleryImgInfo) {
        if (selectedItems.contains(imgInfo)) {
            selectedItems.remove(imgInfo)
        } else {
            selectedItems.add(imgInfo)
        }
        notifyItemRangeChanged(0, itemCount, null)
    }

    fun unselectMode() {
        selectMode = false
        selectedItems.clear()
        notifyItemRangeChanged(0, itemCount, null)
    }
}