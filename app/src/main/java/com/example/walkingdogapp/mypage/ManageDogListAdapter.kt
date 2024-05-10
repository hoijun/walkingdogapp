package com.example.walkingdogapp.mypage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.databinding.ManagedoglistItemBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel

class ManageDogListAdapter(private val dogsList: List<DogInfo>, private val context: Context, private val viewModel: UserInfoViewModel): RecyclerView.Adapter<ManageDogListAdapter.ManageDogListViewHolder>() {

    fun interface OnitemClickListener {
        fun onitemClick(dog: DogInfo)
    }

    var onitemClickListener: OnitemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageDogListViewHolder {
        val binding =
            ManagedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ManageDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = dogsList.size
    override fun onBindViewHolder(holder: ManageDogListViewHolder, position: Int) {
        holder.bind(dogsList[position])
    }

    inner class ManageDogListViewHolder(private val binding: ManagedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: DogInfo) {
            binding.apply {
                manageDogname.text = dog.name
                manageDogfeature.text =
                    "${Constant.getAge(dog.birth)}살/ ${dog.weight}kg / ${dog.breed}"

                if (viewModel.dogsimg.value?.get(dog.name) != null) {
                    Glide.with(context).load(viewModel.dogsimg.value?.get(dog.name))
                        .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(manageDogimg)
                }

                root.setOnClickListener {
                    onitemClickListener?.onitemClick(dog)
                }
            }
        }
    }
}