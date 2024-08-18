package com.example.walkingdogapp.mypage

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Utils
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.databinding.MypagedoglistItemBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.datamodel.DogInfo

class MyPageDogListAdapter(private val dogsList: List<DogInfo>, private val successGetData: Boolean): RecyclerView.Adapter<MyPageDogListAdapter.MyPageDogListViewHolder>() {
    private lateinit var context: Context

    fun interface OnItemClickListener {
        fun onItemClick(dog: DogInfo)
    }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPageDogListViewHolder {
        val binding =
            MypagedoglistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return MyPageDogListViewHolder(binding)
    }

    override fun getItemCount(): Int = if(dogsList.size == 3) 3 else dogsList.size + 1
    override fun onBindViewHolder(holder: MyPageDogListViewHolder, position: Int) {
        if (position == dogsList.size) {
            holder.bind()
            return
        }
        holder.bind(dogsList[position])
    }

    inner class MyPageDogListViewHolder(private val binding: MypagedoglistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.apply {
                menuDogInfo.visibility = View.GONE
                root.setOnClickListener {
                    if (!NetworkManager.checkNetworkState(context) || !successGetData) {
                        return@setOnClickListener
                    }
                    val registerDogIntent = Intent(context, RegisterDogActivity::class.java)
                    context.startActivity(registerDogIntent)
                }
            }
        }

        fun bind(dog: DogInfo) {
            binding.apply {
                mypageAddDogBtn.visibility = View.GONE
                dogInfo = dog
                dogAge = Utils.getAge(dog.birth)

                if (MainActivity.dogUriList[dog.name] != null) {
                    Glide.with(context).load(MainActivity.dogUriList[dog.name])
                        .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(menuDogimg)
                }
                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}