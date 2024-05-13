package com.example.walkingdogapp.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.WalkdateslistItemBinding
import com.example.walkingdogapp.datamodel.WalkRecord

class WalkDatesListAdapter(private val walkRecordList: List<WalkRecord>): RecyclerView.Adapter<WalkDatesListAdapter.WalkdateslistViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(date: WalkRecord)
    }

    var itemClickListener : OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkdateslistViewHolder {
        val binding = WalkdateslistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkdateslistViewHolder(binding)
    }

    override fun getItemCount(): Int = walkRecordList.size

    override fun onBindViewHolder(holder: WalkdateslistViewHolder, position: Int) {
        holder.bind(walkRecordList[position])
    }

    inner class WalkdateslistViewHolder(private val binding: WalkdateslistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(walkRecordList[bindingAdapterPosition])
            }
        }
        fun bind(walkRecord: WalkRecord) {
            binding.apply {
                starttime.text = walkRecord.startTime
                distance.text = "${"%.1f".format(walkRecord.distance / 1000.0)}km"
                time.text = "${(walkRecord.time / 60)}분"
            }
        }
    }
}
