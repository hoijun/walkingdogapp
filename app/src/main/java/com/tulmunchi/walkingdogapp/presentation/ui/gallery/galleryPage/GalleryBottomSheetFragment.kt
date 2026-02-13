package com.tulmunchi.walkingdogapp.presentation.ui.gallery.galleryPage

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tulmunchi.walkingdogapp.databinding.FragmentGalleryBottomSheetBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener

class GalleryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentGalleryBottomSheetBinding? = null
    private val binding get() = _binding!!

    fun interface OnDeleteImgListener {
        fun onDeleteImg()
    }

    var onDeleteImgListener: OnDeleteImgListener? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        when(result.resultCode) {
            Activity.RESULT_OK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dismiss()
                onDeleteImgListener?.onDeleteImg()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentGalleryBottomSheetBinding.inflate(inflater, container, false)
        val date = arguments?.getString("date") ?: "2024년 03월 20일 00:00"
        val imgUri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("uri", Uri::class.java) ?: Uri.EMPTY
        } else {
            arguments?.getParcelable("uri") ?: Uri.EMPTY
        }

        binding.apply {
            day = date
            removeimg.setOnSingleClickListener {
                imgUri?.also {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intentSender = MediaStore.createDeleteRequest(requireActivity().contentResolver, mutableListOf(it)).intentSender
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    } else {
                        val dialog = SelectDialog.newInstance(title = "사진을 삭제하시겠습니까?", showNegativeButton = true)
                        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
                            requireActivity().contentResolver.delete(imgUri, null, null)
                            dismiss()
                            onDeleteImgListener?.onDeleteImg()
                        }
                        dialog.show(parentFragmentManager, "deleteConfirm")
                    }
                }
            }
        }

        this.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}