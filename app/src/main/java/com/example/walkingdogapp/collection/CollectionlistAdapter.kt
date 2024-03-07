package com.example.walkingdogapp.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.CollectionlistItemBinding

class CollectionlistAdapter(private val list: List<String>) : RecyclerView.Adapter<CollectionlistAdapter.CollectionlistViewHolder>() {
    fun interface OnItemClickListener {
        fun onItemClick()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionlistViewHolder {
        val binding = CollectionlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollectionlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionlistViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() : Int = list.size

    inner class CollectionlistViewHolder(private val binding: CollectionlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {

            }
        }
        fun bind(item: String) {
        }
    }
}