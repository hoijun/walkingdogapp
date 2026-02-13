package com.tulmunchi.walkingdogapp.presentation.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.ItemHomeDogListBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog

class HomeDogListAdapter(
    private val dogsList: List<Dog>,
    private val dogImages: Map<String, String>
): RecyclerView.Adapter<HomeDogListAdapter.HomeDogListViewHolder>() {
    lateinit var context: Context

    fun interface OnClickDogListener {
        fun onClickDog(dog: Dog)
    }

    fun interface OnAddDogClickListener {
        fun onAddDogClick()
    }

    private val selectedItems = mutableListOf<String>()
    var onClickDogListener: OnClickDogListener? = null
    var onAddDogClickListener: OnAddDogClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDogListViewHolder {
        val binding = ItemHomeDogListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class HomeDogListViewHolder(private val binding: ItemHomeDogListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dog: Dog) {
            binding.apply {
                dogInfo = dog
                homeAddDogLayout.visibility = View.GONE

                // 체크박스 초기 상태 설정
                walkDogCheckBox.isChecked = selectedItems.contains(dog.name)

                if (dogImages[dog.name] != null) {
                    Glide.with(context).load(dogImages[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(500, 500).into(homeDogImage)
                }

                homeDogLayout.setOnClickListener {
                    onClickDogListener?.onClickDog(dogsList[bindingAdapterPosition])
                    toggleSelection(dogsList[bindingAdapterPosition].name)
                    walkDogCheckBox.isChecked = selectedItems.contains(dogsList[bindingAdapterPosition].name)
                }

                walkDogCheckBox.setOnClickListener {
                    onClickDogListener?.onClickDog(dogsList[bindingAdapterPosition])
                    toggleSelection(dogsList[bindingAdapterPosition].name)
                    walkDogCheckBox.isChecked = selectedItems.contains(dogsList[bindingAdapterPosition].name)
                }
            }
        }

        fun bind() {
            binding.apply {
                homeDogLayout.visibility = View.GONE
                homeAddDogBtn.setOnClickListener {
                    onAddDogClickListener?.onAddDogClick()
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

    fun clearSelection() {
        if (selectedItems.isEmpty()) return

        // 선택된 항목들의 position 찾기
        val positionsToUpdate = mutableListOf<Int>()
        selectedItems.forEach { dogName ->
            val position = dogsList.indexOfFirst { it.name == dogName }
            if (position != -1) {
                positionsToUpdate.add(position)
            }
        }

        selectedItems.clear()

        // 각 position만 업데이트
        positionsToUpdate.forEach { position ->
            notifyItemChanged(position)
        }
    }
}