package com.example.walkingdogapp

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import com.example.walkingdogapp.databinding.WritedogDialogBinding

class WriteDogDialog(context: Context, private val callback : (String) -> Unit) : Dialog(context) {
    private lateinit var binding: WritedogDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WritedogDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resizeDialog()
        setCancelable(false)

        binding.apply {
            finishButton.setOnClickListener {
                callback(writeBreed.text.toString())
                dismiss()
            }

            cancelButton.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        window?.attributes = params as WindowManager.LayoutParams
    }
}