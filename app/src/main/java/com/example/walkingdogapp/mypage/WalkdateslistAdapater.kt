package com.example.walkingdogapp.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.WalkdateslistItemBinding
import com.example.walkingdogapp.datamodel.WalkDate

class WalkdateslistAdapater(private val dates: List<WalkDate>): RecyclerView.Adapter<WalkdateslistAdapater.WalkdateslistViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(date: WalkDate)
    }

    var itemClickListener : OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkdateslistViewHolder {
        val binding = WalkdateslistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkdateslistViewHolder(binding)
    }

    override fun getItemCount(): Int = dates.size

    override fun onBindViewHolder(holder: WalkdateslistViewHolder, position: Int) {
        holder.bind(dates[position])
    }

    inner class WalkdateslistViewHolder(private val binding: WalkdateslistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(dates[bindingAdapterPosition])
            }
        }
        fun bind(walkDate: WalkDate) {
            binding.apply {
                starttime.text = walkDate.startTime
                val kmdistance = "%.1f".format(walkDate.distance / 1000.0)
                distance.text = "${kmdistance}km"
                time.text = "${(walkDate.time / 60)}분"
            }
        }
    }
}
