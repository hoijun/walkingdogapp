package com.tulmunchi.walkingdogapp.mainhome

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.PermissionguideDialogBinding
import com.tulmunchi.walkingdogapp.login.TermOfServiceDialog.OnClickYesListener

class PermissionGuideDialog: DialogFragment() {
    private var _binding: PermissionguideDialogBinding? = null
    private val binding get() = _binding!!

    fun interface OnClickYesListener {
        fun onClickYes()
    }

    var onClickYesListener: OnClickYesListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PermissionguideDialogBinding.inflate(inflater, container, false)
        binding.apply {
            finishButton.setOnClickListener {
                onClickYesListener?.onClickYes()
                dismiss()
            }
        }
        isCancelable = false
        this.dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.95).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}