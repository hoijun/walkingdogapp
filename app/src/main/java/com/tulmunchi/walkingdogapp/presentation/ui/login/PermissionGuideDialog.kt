package com.tulmunchi.walkingdogapp.presentation.ui.login

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.DialogPermissionGuideBinding

class PermissionGuideDialog: DialogFragment() {
    private var _binding: DialogPermissionGuideBinding? = null
    private val binding get() = _binding!!

    fun interface OnClickYesListener {
        fun onClickYes()
    }

    var onClickYesListener: OnClickYesListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogPermissionGuideBinding.inflate(inflater, container, false)
        binding.apply {
            finishButton.setOnClickListener {
                onClickYesListener?.onClickYes()
                dismiss()
            }
        }
        isCancelable = false
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
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
        this.dialog?.window?.setGravity(Gravity.BOTTOM)
        
        // 하단 여백 추가
        val layoutParams = this.dialog?.window?.attributes as WindowManager.LayoutParams
        layoutParams.y = (Resources.getSystem().displayMetrics.density * 20).toInt()
        this.dialog?.window?.attributes = layoutParams
    }
}