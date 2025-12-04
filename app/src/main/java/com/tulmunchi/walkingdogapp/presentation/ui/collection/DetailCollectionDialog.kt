package com.tulmunchi.walkingdogapp.presentation.ui.collection

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.BindingAdapter
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.tulmunchi.walkingdogapp.databinding.DetailcollectionDialogBinding
import com.tulmunchi.walkingdogapp.presentation.model.CollectionInfo

class DetailCollectionDialog: DialogFragment() {
    private var _binding: DetailcollectionDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DetailcollectionDialogBinding.inflate(inflater, container, false)
        val collectionInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("collectionInfo", CollectionInfo::class.java)?: CollectionInfo()
        } else {
            (arguments?.getSerializable("collectionInfo") ?: CollectionInfo()) as CollectionInfo
        }

        binding.collection = collectionInfo


        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    object DialogImageViewBindingAdapter {
        @BindingAdapter("DialogItemImgResId")
        @JvmStatic
        fun loadImage(v: ImageView, resId: Int) {
            Glide.with(v.context).load(resId).centerInside().into(v)
        }
    }
}