package com.example.walkingdogapp.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.WalkdateslistItemBinding
import com.example.walkingdogapp.datamodel.WalkDateInfo

class WalkDatesListAdapter(private val walkDateInfoList: List<WalkDateInfo>): RecyclerView.Adapter<WalkDatesListAdapter.WalkDatesListViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(date: WalkDateInfo)
    }

    var itemClickListener : OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkDatesListViewHolder {
        val binding = WalkdateslistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkDatesListViewHolder(binding)
    }

    override fun getItemCount(): Int = walkDateInfoList.size

    override fun onBindViewHolder(holder: WalkDatesListViewHolder, position: Int) {
        holder.bind(walkDateInfoList[position])
    }

    inner class WalkDatesListViewHolder(private val binding: WalkdateslistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(walkDateInfoList[bindingAdapterPosition])
            }
        }
        fun bind(walkDateInfo: WalkDateInfo) {
            binding.apply {
                walkRecordInfo = walkDateInfo
            }
        }
    }
}
