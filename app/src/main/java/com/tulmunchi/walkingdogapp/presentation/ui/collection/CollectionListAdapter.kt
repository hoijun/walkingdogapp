package com.tulmunchi.walkingdogapp.presentation.ui.collection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.ItemCollectionListBinding
import com.tulmunchi.walkingdogapp.presentation.model.CollectionInfo

class CollectionListAdapter(
    private val collections: List<CollectionInfo>,
    private val collectionWhether: Map<String, Boolean>
) : RecyclerView.Adapter<CollectionListAdapter.CollectionListViewHolder>(), Filterable {
    private lateinit var context: Context
    fun interface OnItemClickListener {
        fun onItemClick(item: CollectionInfo)
    }

    private var collectionInfos = collections

    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionListViewHolder {
        val binding = ItemCollectionListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return CollectionListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionListViewHolder, position: Int) {
        holder.bind(collectionInfos[position])
    }

    override fun getItemCount() : Int = collectionInfos.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class CollectionListViewHolder(private val binding: ItemCollectionListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(collectionInfos[bindingAdapterPosition])
            }
        }
        fun bind(collectionInfo: CollectionInfo) {
            binding.apply {
                collectionItem = collectionInfo
                isOwnedCollection = collectionWhether[collectionInfo.collectionNum] ?: false
                executePendingBindings()
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint.toString()
                val filteredList  = if(charString.isEmpty()) {
                    collections
                } else {
                    // 입력한 값이 도감의 아이템과 관련 있을 경우
                    collections.filter {
                        it.collectionNum.contains(charString) || it.collectionName.contains(charString)
                    }
                }

                return FilterResults().apply { values = filteredList }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                // val oldSize = collectionInfos.size
                collectionInfos = (results.values as List<CollectionInfo>)
                notifyDataSetChanged()
                /* val newSize = collectionInfos.size
                if (oldSize == newSize) {
                    notifyItemRangeChanged(0, newSize)
                } else if (oldSize > newSize) {
                    notifyItemRangeChanged(0, newSize)
                    notifyItemRangeRemoved(newSize, oldSize - newSize)
                } else {
                    notifyItemRangeChanged(0, oldSize)
                    notifyItemRangeInserted(oldSize, newSize - oldSize)
                }*/
            }
        }
    }

    object CollectionItemImageViewBindingAdapter {
        @BindingAdapter("CollectionItemImgResId", "IsExistedCollection")
        @JvmStatic
        fun loadImage(iv: ImageView, resId: Int, isOwned: Boolean) {
            if (iv.context == null) return
            try {
                if (isOwned) {
                    Glide.with(iv.context).load(resId).format(DecodeFormat.PREFER_ARGB_8888)
                        .override(400, 400).error(R.drawable.collection_unobtained).into(iv)
                } else {
                    Glide.with(iv.context).load(R.drawable.collection_unobtained)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(400, 400).into(iv)
                }
            } catch (e: Exception) {
                iv.setImageResource(R.drawable.collection_unobtained)
            }
        }
    }
}