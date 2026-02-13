package com.tulmunchi.walkingdogapp.presentation.core.dialog

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.DialogSelectBinding

class SelectDialog : DialogFragment() {
    private var _binding: DialogSelectBinding? = null
    private val binding get() = _binding!!

    /**
     * 확인 버튼 클릭 리스너
     */
    fun interface OnConfirmListener {
        fun onConfirm()
    }

    /**
     * 취소 버튼 클릭 리스너
     */
    fun interface OnCancelListener {
        fun onCancel()
    }

    var onConfirmListener: OnConfirmListener? = null
    var onCancelListener: OnCancelListener? = null

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_SHOW_NEGATIVE_BUTTON = "show_negative_button"

        /**
         * @param title 다이얼로그 제목 (기본값: "나가시겠어요?")
         * @param showNegativeButton "아니요" 버튼 표시 여부 (기본값: false)
         */
        fun newInstance(
            title: String = "나가시겠어요?",
            showNegativeButton: Boolean = false
        ): SelectDialog {
            return SelectDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_SHOW_NEGATIVE_BUTTON, showNegativeButton)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectBinding.inflate(inflater, container, false)

        val title = arguments?.getString(ARG_TITLE) ?: "나가시겠어요?"
        val showNegativeButton = arguments?.getBoolean(ARG_SHOW_NEGATIVE_BUTTON) ?: false

        binding.apply {
            dialogTitle.text = title

            negativeBtn.visibility = if (showNegativeButton) View.VISIBLE else View.GONE

            positiveBtn.setOnClickListener {
                onConfirmListener?.onConfirm()
                dismiss()
            }

            negativeBtn.setOnClickListener {
                onCancelListener?.onCancel()
                dismiss()
            }
        }

        // 다이얼로그 외부 터치 시 닫히지 않도록 설정
        this.dialog?.setCanceledOnTouchOutside(false)
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}
