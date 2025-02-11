package com.tulmunchi.walkingdogapp.mypage

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.databinding.WalkinfodoglistItemBinding
import com.tulmunchi.walkingdogapp.datamodel.DogInfo

class WalkInfoDogListAdapter(private val dogsInfo: List<DogInfo>, private val dogsImg: HashMap<String, Uri>):  RecyclerView.Adapter<WalkInfoDogListAdapter.WalkInfoDogListViewHolder>(){
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

    override fun getItemCount(): Int = dogsInfo.size

    override fun onBindViewHolder(holder: WalkInfoDogListViewHolder, position: Int) {
        holder.bind(dogsInfo[position])
    }

    inner class WalkInfoDogListViewHolder(private val binding: WalkinfodoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: DogInfo) {
            if (dogsImg.get(dog.name) != null) {
                Glide.with(context).load(dogsImg.get(dog.name))
                    .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(binding.WalkInfoDogImg)
            }

            binding.root.setOnClickListener {
                onItemClickListener?.onItemClick(dog)
            }
        }
    }
}