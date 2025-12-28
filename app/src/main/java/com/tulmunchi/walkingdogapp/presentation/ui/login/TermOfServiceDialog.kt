package com.tulmunchi.walkingdogapp.presentation.ui.login

import android.content.Intent
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
import com.tulmunchi.walkingdogapp.databinding.DialogTermofserviceBinding
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.settingPage.PrivacyWebViewActivity
import androidx.core.graphics.toColorInt

class TermOfServiceDialog: DialogFragment() {
    private var _binding: DialogTermofserviceBinding? = null
    private val binding get() = _binding!!
    fun interface OnClickYesListener {
        fun onClick(agree: Boolean)
    }

    var onClickYesListener: OnClickYesListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogTermofserviceBinding.inflate(inflater, container, false)
        binding.apply {
            useService.setOnCheckedChangeListener { _, _ ->
                updateCheckStatus()
            }

            userInfoService.setOnCheckedChangeListener { _, _ ->
                updateCheckStatus()
            }

            locationService.setOnCheckedChangeListener { _, _ ->
                updateCheckStatus()
            }

            allCheck.setOnClickListener {
                val isChecked = allCheck.isChecked
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
        this.dialog?.window?.setGravity(Gravity.BOTTOM)

        // 하단 여백 추가
        val layoutParams = this.dialog?.window?.attributes as WindowManager.LayoutParams
        layoutParams.y = (Resources.getSystem().displayMetrics.density * 20).toInt()
        this.dialog?.window?.attributes = layoutParams
    }

    private fun openWebView(url: String) {
        val intent = Intent(context, PrivacyWebViewActivity::class.java).apply {
            putExtra("uri", url)
        }
        context?.startActivity(intent)
    }

    private fun updateCheckStatus() {
        binding.apply {
            val allChecked = useService.isChecked && userInfoService.isChecked && locationService.isChecked
            allCheck.isChecked = allChecked
            updateFinishButtonState(allChecked)
        }
    }


    private fun updateFinishButtonState(isEnabled: Boolean) {
        binding.finishButton.apply {
            this.isEnabled = isEnabled
            setTextColor(if (isEnabled) Color.BLACK else "#D3D3D3".toColorInt())
        }
    }
}