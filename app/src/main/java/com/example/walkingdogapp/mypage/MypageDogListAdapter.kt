package com.example.walkingdogapp.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.databinding.MypagedoglistItemBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.userinfo.DogInfo
import com.example.walkingdogapp.userinfo.UserInfoViewModel

class MypageDogListAdapter(private val dogsList: List<DogInfo>, private val context: Context, private val viewModel: UserInfoViewModel): RecyclerView.Adapter<MypageDogListAdapter.MypageDogListViewHolder>() {

    fun interface OnitemClickListener {
        fun onitemClick(dog: DogInfo)
    }

    var onitemClickListener: OnitemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MypageDogListViewHolder {
        val binding =
            MypagedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MypageDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = if(dogsList.size == 3) 3 else dogsList.size + 1
    override fun onBindViewHolder(holder: MypageDogListViewHolder, position: Int) {
        if (position == dogsList.size) {
            holder.bind()
            return
        }
        holder.bind(dogsList[position])
    }

    inner class MypageDogListViewHolder(private val binding: MypagedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.apply {
                menuDogInfo.visibility = View.GONE
                root.setOnClickListener {
                    val registerDogIntent = Intent(context, RegisterDogActivity::class.java)
                    context.startActivity(registerDogIntent)
                }
            }
        }

        fun bind(dog: DogInfo) {
            binding.apply {
                mypageAddDogBtn.visibility = View.GONE
                menuDogname.text = dog.name
                menuDogfeature.text =
                    "${Constant.getAge(dog.birth)}살/ ${dog.weight}kg / ${dog.breed}"
                if (viewModel.dogsimg.value?.get(dog.name) != null) {
                    Glide.with(context).load(viewModel.dogsimg.value?.get(dog.name))
                        .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(menuDogimg)
                }
                root.setOnClickListener {
                    onitemClickListener?.onitemClick(dog)
                }
            }
        }
    }
}