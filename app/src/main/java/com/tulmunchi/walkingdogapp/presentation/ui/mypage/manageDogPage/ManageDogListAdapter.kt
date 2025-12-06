package com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.databinding.ManagedoglistItemBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils

class ManageDogListAdapter(
    private val dogsList: List<Dog>,
    private val dogImages: Map<String, String>
): RecyclerView.Adapter<ManageDogListAdapter.ManageDogListViewHolder>() {
    private lateinit var context: Context
    fun interface OnItemClickListener {
        fun onItemClick(dog: Dog)
    }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageDogListViewHolder {
        val binding =
            ManagedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return ManageDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = dogsList.size
    override fun onBindViewHolder(holder: ManageDogListViewHolder, position: Int) {
        holder.bind(dogsList[position])
    }

    inner class ManageDogListViewHolder(private val binding: ManagedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: Dog) {
            binding.apply {
                dogInfo = dog
                dogAge = DateUtils.getAge(dog.birth)

                if (dogImages[dog.name] != null) {
                    Glide.with(context).load(dogImages[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(manageDogImg)
                }

                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}