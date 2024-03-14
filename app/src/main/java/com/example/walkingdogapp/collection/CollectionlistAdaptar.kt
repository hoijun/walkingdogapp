package com.example.walkingdogapp.collection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.CollectionlistItemBinding

class CollectionlistAdaptar(private val collections: List<CollectionInfo>, private val context: Context) : RecyclerView.Adapter<CollectionlistAdaptar.CollectionlistViewHolder>(), Filterable {
    fun interface OnItemClickListener {
        fun onItemClick(item: CollectionInfo)
    }

    private var collectionsInfo: List<CollectionInfo> = collections

    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionlistViewHolder {
        val binding = CollectionlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollectionlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionlistViewHolder, position: Int) {
        holder.bind(collectionsInfo[position])
    }

    override fun getItemCount() : Int = collectionsInfo.size

    inner class CollectionlistViewHolder(private val binding: CollectionlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(collectionsInfo[bindingAdapterPosition])
            }
        }
        fun bind(item: CollectionInfo) {
            binding.apply {
                Glide.with(context).load(item.collectionImg).format(DecodeFormat.PREFER_RGB_565).override(400, 400).into(collectionImg)
                collectionNumber.text = item.collectionNum
                collectionName.text = item.collectionName
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint.toString()
                collectionsInfo = if (charString.isEmpty()) {
                    collections
                } else {
                    // 입력한 값이 도감의 아이템과 관련 있을 경우
                    val filterdList = mutableListOf<CollectionInfo>()
                    for(item in collections) {
                        for(keyword in item.keywords) {
                            if(keyword.contains(charString)) {
                                filterdList.add(item)
                                break
                            }
                        }
                        if(item.collectionNum.contains(charString)) {
                            filterdList.add(item)
                        }
                    }
                    filterdList
                }
                val filterResult = FilterResults()
                filterResult.values = collectionsInfo
                return filterResult
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                collectionsInfo = results.values as List<CollectionInfo>
                notifyDataSetChanged()
            }
        }
    }
}