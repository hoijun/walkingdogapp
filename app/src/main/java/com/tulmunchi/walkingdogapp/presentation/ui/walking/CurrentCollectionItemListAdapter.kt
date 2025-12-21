package com.tulmunchi.walkingdogapp.presentation.ui.walking

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tulmunchi.walkingdogapp.databinding.ItemCurrentCollectionBinding
import com.tulmunchi.walkingdogapp.presentation.model.CollectionInfo

class CurrentCollectionItemListAdapter(private val getCollectionItemInfos: MutableList<CollectionInfo>): RecyclerView.Adapter<CurrentCollectionItemListAdapter.GetCollectionItemListViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): GetCollectionItemListViewHolder {
        val binding = ItemCurrentCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return GetCollectionItemListViewHolder(binding)
    }

    override fun getItemCount() = getCollectionItemInfos.size

    override fun onBindViewHolder(holder: GetCollectionItemListViewHolder, position: Int) {
        holder.bind(getCollectionItemInfos[position])
    }

    inner class GetCollectionItemListViewHolder(private val binding: ItemCurrentCollectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(collectionInfo: CollectionInfo) {
            Glide.with(context).load(collectionInfo.collectionResId).into(binding.getCollectionImg)
        }
    }

}