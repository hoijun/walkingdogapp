package com.tulmunchi.walkingdogapp.presentation.ui.login

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.TermofserviceDialogBinding
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.settingPage.PrivacyWebViewActivity
import androidx.core.graphics.toColorInt

class TermOfServiceDialog: DialogFragment() {
    private var _binding: TermofserviceDialogBinding? = null
    private val binding get() = _binding!!
    fun interface OnClickYesListener {
        fun onClick(agree: Boolean)
    }

    private var isCheckedFirst = false
    private var isCheckedSecond = false
    private var isCheckedThird = false

    var onClickYesListener: OnClickYesListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = TermofserviceDialogBinding.inflate(inflater, container, false)
        binding.apply {
            useService.setOnCheckedChangeListener { _, isChecked ->
                isCheckedFirst = isChecked
                updateCheckStatus()

            }

            userInfoService.setOnCheckedChangeListener { _, isChecked ->
                isCheckedSecond = isChecked
                updateCheckStatus()
            }

            locationService.setOnCheckedChangeListener { _, isChecked ->
                isCheckedThird = isChecked
                updateCheckStatus()
            }

            allCheck.setOnCheckedChangeListener { _, isChecked ->
                useService.isChecked = isChecked
                userInfoService.isChecked = isChecked
                locationService.isChecked = isChecked
                updateFinishButtonState(isChecked)
            }

            toUseServiceDetail.setOnClickListener {
                openWebView("https://hoitho.tistory.com/1")
            }

            toUserInfoServiceDetail.setOnClickListener {
                openWebView("https://hoitho.tistory.com/2")
            }

            toLocationServiceDetail.setOnClickListener {
                openWebView("https://hoitho.tistory.com/3")
            }

            cancelButton.setOnClickListener {
                dismiss()
            }

            finishButton.setOnClickListener {
                onClickYesListener?.onClick(true)
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
    }

    private fun openWebView(url: String) {
        val intent = Intent(context, PrivacyWebViewActivity::class.java).apply {
            putExtra("uri", url)
        }
        context?.startActivity(intent)
    }

    private fun updateCheckStatus() {
        binding.apply {
            if (isCheckedFirst && isCheckedSecond && isCheckedThird) {
                allCheck.isChecked = true
            }

            if (!isCheckedFirst && !isCheckedSecond && !isCheckedThird) {
                allCheck.isChecked = false
            }

            updateFinishButtonState(allCheck.isChecked)
        }
    }


    private fun updateFinishButtonState(isEnabled: Boolean) {
        binding.finishButton.apply {
            this.isEnabled = isEnabled
            setTextColor(if (isEnabled) Color.BLACK else "#D3D3D3".toColorInt())
        }
    }
}