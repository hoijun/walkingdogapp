package com.example.walkingdogapp

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.walkingdogapp.databinding.WriteDialogBinding

class WriteDialog : DialogFragment() {
    private lateinit var binding: WriteDialogBinding

    fun interface OnClickYesListener {
        fun onClick(text: String)
    }

    var clickYesListener: OnClickYesListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WriteDialogBinding.inflate(inflater, container, false)
        val writeText = arguments?.getString("text")?: ""
        binding.apply {
            write.hint = writeText
            finishButton.setOnClickListener {
                clickYesListener?.onClick(write.text.toString())
                dismiss()
            }

            cancelButton.setOnClickListener {
                dismiss()
            }
        }
        isCancelable = false
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