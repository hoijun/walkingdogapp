package com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.ItemMypageDogListBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils

class MyPageDogListAdapter(
    private val dogsList: List<Dog>,
    private val successGetData: Boolean,
    private val networkChecker: NetworkChecker,
    private val dogImages: Map<String, String>
): RecyclerView.Adapter<MyPageDogListAdapter.MyPageDogListViewHolder>() {
    private lateinit var context: Context

    fun interface OnItemClickListener {
        fun onItemClick(dog: Dog)
    }

    fun interface OnAddDogClickListener {
        fun onAddDogClick()
    }

    var onItemClickListener: OnItemClickListener? = null
    var onAddDogClickListener: OnAddDogClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPageDogListViewHolder {
        val binding =
            ItemMypageDogListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class MyPageDogListViewHolder(private val binding: ItemMypageDogListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.apply {
                menuDogInfo.visibility = View.GONE
                root.setOnClickListener {
                    if (!networkChecker.isNetworkAvailable() || !successGetData) {
                        return@setOnClickListener
                    }
                    onAddDogClickListener?.onAddDogClick()
                }
            }
        }

        fun bind(dog: Dog) {
            binding.apply {
                btnMyPageAddDog.visibility = View.GONE
                dogInfo = dog
                dogAge = DateUtils.getAge(dog.birth)

                if (dogImages[dog.name] != null) {
                    Glide.with(context).load(dogImages[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(menuDogImg)
                }
                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}