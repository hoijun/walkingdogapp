package com.example.walkingdogapp.mypage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.ManagedoglistItemBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel

class ManageDogListAdapter(private val dogsList: List<DogInfo>): RecyclerView.Adapter<ManageDogListAdapter.ManageDogListViewHolder>() {
    private lateinit var context: Context
    fun interface OnItemClickListener {
        fun onItemClick(dog: DogInfo)
    }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageDogListViewHolder {
        val binding =
            ManagedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return ManageDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = dogsList.size ?: 0
    override fun onBindViewHolder(holder: ManageDogListViewHolder, position: Int) {
        holder.bind(dogsList[position])
    }

    inner class ManageDogListViewHolder(private val binding: ManagedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: DogInfo) {
            binding.apply {
                dogInfo = dog
                dogAge = Constant.getAge(dog.birth)

                if (MainActivity.dogUriList[dog.name] != null) {
                    Glide.with(context).load(MainActivity.dogUriList[dog.name])
                        .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(manageDogimg)
                }

                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}