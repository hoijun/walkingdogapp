package com.example.walkingdogapp

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import com.example.walkingdogapp.databinding.WriteDialogBinding

class WriteDialog(context: Context, private val writeText : String, private val callback : (String) -> Unit) : Dialog(context) {
    private lateinit var binding: WriteDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WriteDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resizeDialog()
        setCancelable(false)

        binding.apply {
            write.hint = writeText
            finishButton.setOnClickListener {
                callback(write.text.toString())
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