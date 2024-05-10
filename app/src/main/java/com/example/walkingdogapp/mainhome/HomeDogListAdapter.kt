package com.example.walkingdogapp.mainhome

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.databinding.HomedoglistItemBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel

class HomeDogListAdapter(private val dogsList: List<DogInfo>, private val context: Context, private val viewModel: UserInfoViewModel): RecyclerView.Adapter<HomeDogListAdapter.HomeDogListViewHolder>() {

    fun interface OnClickDogListener {
        fun onClickDog(dogName: String)
    }

    private val selectedItems = mutableListOf<String>()
    var onClickDogListener: OnClickDogListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDogListViewHolder {
        val binding = HomedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = if(dogsList.size == 3) 3 else dogsList.size + 1
    override fun onBindViewHolder(holder: HomeDogListViewHolder, position: Int) {
        if (position == dogsList.size) {
            holder.bind()
            return
        }
        holder.bind(dogsList[position])
    }

    inner class HomeDogListViewHolder(private val binding: HomedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: DogInfo) {
            binding.apply {
                dogCheckBox.background = null
                homeAddDogBtn.visibility = View.GONE
                homeDogName.text = dog.name
                if (viewModel.dogsimg.value?.get(dog.name) != null) {
                    Glide.with(context).load(viewModel.dogsimg.value?.get(dog.name))
                        .format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(homeDogImage)
                }

                homeDogLayout.setOnClickListener {
                    onClickDogListener?.onClickDog(dogsList[bindingAdapterPosition].name)
                    toggleSelection(dogsList[bindingAdapterPosition].name)
                    dogCheckBox.isChecked = selectedItems.contains(dogsList[bindingAdapterPosition].name)
                }

                dogCheckBox.setOnClickListener {
                    onClickDogListener?.onClickDog(dogsList[bindingAdapterPosition].name)
                    toggleSelection(dogsList[bindingAdapterPosition].name)
                }
            }
        }

        fun bind() {
            binding.apply {
                homeDogLayout.visibility = View.GONE
                homeAddDogBtn.setOnClickListener {
                    val registerDogIntent = Intent(context, RegisterDogActivity::class.java)
                    context.startActivity(registerDogIntent)
                }
            }
        }
    }

    private fun toggleSelection(dogName: String) {
        if (selectedItems.contains(dogName)) {
            selectedItems.remove(dogName)
        } else {
            selectedItems.add(dogName)
        }
    }
}