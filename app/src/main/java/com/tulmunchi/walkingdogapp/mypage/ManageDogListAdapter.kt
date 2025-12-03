package com.tulmunchi.walkingdogapp.mypage

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.databinding.ManagedoglistItemBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.utils.Utils

class ManageDogListAdapter(private val dogsList: List<Dog>): RecyclerView.Adapter<ManageDogListAdapter.ManageDogListViewHolder>() {
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
                dogAge = Utils.getAge(dog.birth)

                if (MainActivity.dogImageUrls[dog.name] != null) {
                    Glide.with(context).load(MainActivity.dogImageUrls[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(manageDogImg)
                }

                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}