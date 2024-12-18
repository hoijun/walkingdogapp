package com.example.walkingdogapp.mypage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.WalkinfodoglistItemBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.viewmodel.MainViewModel

class WalkInfoDogListAdapter(private val viewModel: MainViewModel):  RecyclerView.Adapter<WalkInfoDogListAdapter.WalkInfoDogListViewHolder>(){
    private lateinit var context: Context
    fun interface OnItemClickListener {
        fun onItemClick(dog: DogInfo)
    }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkInfoDogListViewHolder {
        context = parent.context
        val binding = WalkinfodoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkInfoDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = viewModel.dogsInfo.value?.size ?: 0

    override fun onBindViewHolder(holder: WalkInfoDogListViewHolder, position: Int) {
        holder.bind(viewModel.dogsInfo.value?.get(position) ?: DogInfo())
    }

    inner class WalkInfoDogListViewHolder(private val binding: WalkinfodoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: DogInfo) {
            if (viewModel.dogsImg.value?.get(dog.name) != null) {
                Glide.with(context).load(viewModel.dogsImg.value?.get(dog.name))
                    .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(binding.WalkInfoDogImg)
            }

            binding.root.setOnClickListener {
                onItemClickListener?.onItemClick(dog)
            }
        }
    }
}