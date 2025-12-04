package com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import com.tulmunchi.walkingdogapp.databinding.MypagedoglistItemBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.register.registerDogPage.RegisterDogActivity

class MyPageDogListAdapter(
    private val dogsList: List<Dog>,
    private val successGetData: Boolean,
    private val networkChecker: NetworkChecker
): RecyclerView.Adapter<MyPageDogListAdapter.MyPageDogListViewHolder>() {
    private lateinit var context: Context

    fun interface OnItemClickListener {
        fun onItemClick(dog: Dog)
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
                    if (!networkChecker.isNetworkAvailable() || !successGetData) {
                        return@setOnClickListener
                    }
                    val registerDogIntent = Intent(context, RegisterDogActivity::class.java)
                    context.startActivity(registerDogIntent)
                }
            }
        }

        fun bind(dog: Dog) {
            binding.apply {
                myPageAddDogBtn.visibility = View.GONE
                dogInfo = dog
                dogAge = DateUtils.getAge(dog.birth)

                if (MainActivity.dogImageUrls[dog.name] != null) {
                    Glide.with(context).load(MainActivity.dogImageUrls[dog.name])
                        .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(menuDogimg)
                }
                root.setOnClickListener {
                    onItemClickListener?.onItemClick(dog)
                }
            }
        }
    }
}