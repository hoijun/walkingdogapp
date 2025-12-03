package com.tulmunchi.walkingdogapp.mainhome

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.HomedoglistItemBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.registerinfo.RegisterDogActivity

class HomeDogListAdapter(
    private val dogsList: List<Dog>,
    private val successGetData: Boolean,
    private val networkChecker: NetworkChecker
): RecyclerView.Adapter<HomeDogListAdapter.HomeDogListViewHolder>() {
    lateinit var context: Context

    fun interface OnClickDogListener {
        fun onClickDog(dogName: String)
    }

    private val selectedItems = mutableListOf<String>()
    var onClickDogListener: OnClickDogListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDogListViewHolder {
        val binding = HomedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
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
        fun bind(dog: Dog) {
            binding.apply {
                dogInfo = dog
                homeAddDogBtn.visibility = View.GONE
                if (MainActivity.dogImageUrls[dog.name] != null) {
                    Glide.with(context).load(MainActivity.dogImageUrls[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(500, 500).into(homeDogImage)
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
                    if(!networkChecker.isNetworkAvailable() || !successGetData) {
                        return@setOnClickListener
                    }
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