package com.example.walkingdogapp.collection

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.walkingdogapp.databinding.DetailcollectionDialogBinding
class DetailCollectionDialog: DialogFragment() {
    private lateinit var binding: DetailcollectionDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DetailcollectionDialogBinding.inflate(inflater, container, false)
        val collectionInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("collectionInfo", CollectionInfo::class.java)?: CollectionInfo()
        } else {
            (arguments?.getSerializable("collectionInfo") ?: CollectionInfo()) as CollectionInfo
        }

        binding.apply {
            Glide.with(requireContext()).load(collectionInfo.collectionImg).into(collectionImg)
            collectionNumber.text = collectionInfo.collectionNum
            collectionName.text = collectionInfo.collectionName
            collectionText.text = collectionInfo.collectionText
        }

        this.dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        resizeDialog()
        return binding.root
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}