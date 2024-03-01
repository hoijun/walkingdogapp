package com.example.walkingdogapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.WalkdateslistItemBinding

class WalkdateslistAdapater(private val dates: List<Walkdate>): RecyclerView.Adapter<WalkdateslistAdapater.WalkdateslisViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkdateslisViewHolder {
        val binding = WalkdateslistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkdateslisViewHolder(binding)
    }

    override fun getItemCount(): Int = dates.size

    override fun onBindViewHolder(holder: WalkdateslisViewHolder, position: Int) {
        holder.bind(dates[position])
    }

    inner class WalkdateslisViewHolder(private val binding: WalkdateslistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {

            }
        }
        fun bind(walkdate: Walkdate) {
            binding.apply {
                starttime.text = walkdate.startTime
                val kmdistance = "%.1f".format(walkdate.distance / 1000.0)
                distance.text = "${kmdistance}km"
                time.text = "${(walkdate.time / 60)}분"
            }
        }
    }
}
