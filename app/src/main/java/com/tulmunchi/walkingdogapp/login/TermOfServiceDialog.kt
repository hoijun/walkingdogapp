package com.tulmunchi.walkingdogapp.login

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.TermofserviceDialogBinding
import com.tulmunchi.walkingdogapp.mypage.PrivacyWebViewActivity

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
        savedInstanceState: Bundle?
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
                openWebView("https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9%EC%95%BD%EA%B4%80")
            }

            toUserInfoServiceDetail.setOnClickListener {
                openWebView("https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EA%B0%9C%EC%9D%B8%EC%A0%95%EB%B3%B4-%EC%B2%98%EB%A6%AC-%EB%B0%A9%EC%B9%A8#")
            }

            toLocationServiceDetail.setOnClickListener {
                openWebView("https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EC%9C%84%EC%B9%98-%EA%B8%B0%EB%B0%98-%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9%EC%95%BD%EA%B4%80")
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
            setTextColor(if (isEnabled) Color.BLACK else Color.parseColor("#D3D3D3"))
        }
    }
}