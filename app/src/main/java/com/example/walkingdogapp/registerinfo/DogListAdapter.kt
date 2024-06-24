package com.example.walkingdogapp.registerinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.DoglistDialogItemBinding

class DogListAdapter(private val dogs: List<String>) : RecyclerView.Adapter<DogListAdapter.DogListViewHolder>() {
    fun interface OnItemClickListener {
        fun onItemClick(name:String)
    }

    var itemClickListener : OnItemClickListener? = null

    // ViewHolder 생성하는 함수, 최소 생성 횟수만큼만 호출됨 (계속 호출 X)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogListViewHolder {
        val binding = DoglistDialogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DogListViewHolder(binding)
    }

    // 만들어진 ViewHolder에 데이터를 바인딩하는 함수
    // position = 리스트 상에서 몇번째인지 의미
    override fun onBindViewHolder(holder: DogListViewHolder, position: Int) {
        holder.bind(dogs[position])
    }

    override fun getItemCount() : Int = dogs.size

    inner class DogListViewHolder(private val binding: DoglistDialogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    itemClickListener?.onItemClick(dogs[bindingAdapterPosition])
                }
            }
            fun bind(dog: String) {
                binding.breed = dog
            }
    }
}