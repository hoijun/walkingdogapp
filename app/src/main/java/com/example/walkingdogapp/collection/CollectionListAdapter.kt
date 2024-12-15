package com.example.walkingdogapp.collection

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.CollectionlistItemBinding
import com.example.walkingdogapp.datamodel.CollectionInfo

class CollectionListAdapter(
    private val collections: List<CollectionInfo>,
    private val collectionWhether: HashMap<String, Boolean>
) : RecyclerView.Adapter<CollectionListAdapter.CollectionListViewHolder>(), Filterable {
    private lateinit var context: Context
    fun interface OnItemClickListener {
        fun onItemClick(item: CollectionInfo)
    }

    private var collectionInfos = collections

    var itemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionListViewHolder {
        val binding = CollectionlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class CollectionListViewHolder(private val binding: CollectionlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemClick(collectionInfos[bindingAdapterPosition])
            }
        }
        fun bind(collectionInfo: CollectionInfo) {
            binding.apply {
                collectionItem = collectionInfo
                isOwnedCollection = collectionWhether[collectionInfo.collectionNum]
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
                Log.d("savepoint", collectionInfos.toString())
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
            if (isOwned) {
                Glide.with(iv.context).load(resId).format(DecodeFormat.PREFER_RGB_565)
                    .override(400, 400).into(iv)
            } else {
                Glide.with(iv.context).load(R.drawable.waitimage).format(DecodeFormat.PREFER_RGB_565)
                    .override(400, 400).into(iv)
            }
        }
    }
}