package com.example.walkingdogapp.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.album.AlbumMapImgInfo
import com.example.walkingdogapp.databinding.AlarmlistItemBinding

class AlarmlistAdapter(private val alarmList: MutableList<String>): RecyclerView.Adapter<AlarmlistAdapter.AlarmitemlistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmitemlistViewHolder {
        val binding = AlarmlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmitemlistViewHolder(binding)
    }

    override fun getItemCount(): Int = alarmList.size

    override fun onBindViewHolder(holder: AlarmitemlistViewHolder, position: Int) {
        holder.bind()
    }

    inner class AlarmitemlistViewHolder(private val binding: AlarmlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {

            }
        }
        fun bind() {

        }
    }
}