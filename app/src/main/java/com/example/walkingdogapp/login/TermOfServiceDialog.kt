package com.example.walkingdogapp.login

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
import com.example.walkingdogapp.databinding.TermofserviceDialogBinding
import com.example.walkingdogapp.mypage.PrivacyWebViewActivity

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

                if(isCheckedFirst && isCheckedSecond && isCheckedThird) {
                    allCheck.isChecked = true
                }

                if(!isCheckedFirst && !isCheckedSecond && !isCheckedThird) {
                    allCheck.isChecked = false
                }

                if(allCheck.isChecked) {
                    finishButton.apply {
                        isEnabled = true
                        setTextColor(Color.BLACK)
                    }
                }
            }

            userInfoService.setOnCheckedChangeListener { _, isChecked ->
                isCheckedSecond = isChecked

                if(isCheckedFirst && isCheckedSecond && isCheckedThird) {
                    allCheck.isChecked = true
                }

                if(!isCheckedFirst && !isCheckedSecond && !isCheckedThird) {
                    allCheck.isChecked = false
                }

                if(allCheck.isChecked) {
                    finishButton.apply {
                        isEnabled = true
                        setTextColor(Color.BLACK)
                    }
                }
            }

            locationService.setOnCheckedChangeListener { _, isChecked ->
                isCheckedThird = isChecked

                if(isCheckedFirst && isCheckedSecond && isCheckedThird) {
                    allCheck.isChecked = true
                }

                if(!isCheckedFirst && !isCheckedSecond && !isCheckedThird) {
                    allCheck.isChecked = false
                }

                if(allCheck.isChecked) {
                    finishButton.apply {
                        isEnabled = true
                        setTextColor(Color.BLACK)
                    }
                }
            }



            allCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    useService.isChecked = true
                    userInfoService.isChecked = true
                    locationService.isChecked = true
                    finishButton.apply {
                        isEnabled = true
                        setTextColor(Color.BLACK)
                    }
                } else {
                    useService.isChecked = false
                    userInfoService.isChecked = false
                    locationService.isChecked = false
                    finishButton.apply {
                        isEnabled = false
                        setTextColor(Color.parseColor("#D3D3D3"))
                    }
                }
            }

            toUseServiceDetail.setOnClickListener {
                val intent = Intent(requireContext(), PrivacyWebViewActivity::class.java).apply {
                    putExtra("uri", "https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9%EC%95%BD%EA%B4%80")
                }
                startActivity(intent)
            }

            toUserInfoServiceDetail.setOnClickListener {
                val intent = Intent(requireContext(), PrivacyWebViewActivity::class.java).apply {
                    putExtra("uri", "https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EA%B0%9C%EC%9D%B8%EC%A0%95%EB%B3%B4-%EC%B2%98%EB%A6%AC-%EB%B0%A9%EC%B9%A8#")
                }
                startActivity(intent)
            }

            toLocationServiceDetail.setOnClickListener {
                val intent = Intent(requireContext(), PrivacyWebViewActivity::class.java).apply {
                    putExtra("uri", "https://velog.io/@ghlwns10/%ED%84%B8%EB%AD%89%EC%B9%98-%EC%9C%84%EC%B9%98-%EA%B8%B0%EB%B0%98-%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9%EC%95%BD%EA%B4%80")
                }
                startActivity(intent)
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
}