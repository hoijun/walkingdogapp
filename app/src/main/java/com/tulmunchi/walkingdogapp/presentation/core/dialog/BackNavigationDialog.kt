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
import com.tulmunchi.walkingdogapp.databinding.DialogBackNavigationBinding

/**
 * 뒤로가기 전용 다이얼로그
 * 사용자가 뒤로가기 버튼을 눌렀을 때 나가기를 확인하는 다이얼로그
 */
class BackNavigationDialog : DialogFragment() {
    private var _binding: DialogBackNavigationBinding? = null
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

        /**
         * BackNavigationDialog 인스턴스 생성
         * @param title 다이얼로그 제목 (기본값: "나가시겠어요?")
         */
        fun newInstance(
            title: String = "나가시겠어요?"
        ): BackNavigationDialog {
            return BackNavigationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBackNavigationBinding.inflate(inflater, container, false)

        val title = arguments?.getString(ARG_TITLE) ?: "나가시겠어요?"

        binding.apply {
            dialogTitle.text = title

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
