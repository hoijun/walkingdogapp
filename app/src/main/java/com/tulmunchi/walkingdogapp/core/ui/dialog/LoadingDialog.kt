package com.tulmunchi.walkingdogapp.core.ui.dialog

import androidx.fragment.app.FragmentManager
import com.tulmunchi.walkingdogapp.common.LoadingDialogFragment
import com.tulmunchi.walkingdogapp.utils.Utils.Companion.LOADING_DIALOG_TAG
import javax.inject.Inject

/**
 * 로딩 다이얼로그 인터페이스
 * 기존 LoadingDialogFragment를 래핑하여 사용
 */
interface LoadingDialog {
    /**
     * 로딩 다이얼로그 표시
     */
    fun show()

    /**
     * 로딩 다이얼로그 숨김
     */
    fun dismiss()
}

/**
 * LoadingDialog 구현체
 * 기존 LoadingDialogFragment를 내부적으로 사용
 */
class LoadingDialogImpl(
    private val fragmentManager: FragmentManager
) : LoadingDialog {

    private var dialogFragment: LoadingDialogFragment? = null

    override fun show() {
        try {
            if (dialogFragment == null || dialogFragment?.isAdded == false) {
                dialogFragment = LoadingDialogFragment()
                dialogFragment?.show(fragmentManager, LOADING_DIALOG_TAG)
            }
        } catch (e: IllegalStateException) {
            // FragmentManager가 이미 destroyed된 경우 무시
        }
    }

    override fun dismiss() {
        try {
            dialogFragment?.let {
                if (it.isAdded && !it.isDetached) {
                    it.dismissAllowingStateLoss()
                }
            }
        } catch (e: IllegalStateException) {
            // Fragment가 이미 FragmentManager와 연결이 끊긴 경우 무시
        } finally {
            dialogFragment = null
        }
    }
}

/**
 * LoadingDialog 팩토리
 * Fragment마다 별도의 LoadingDialog 인스턴스를 생성하기 위한 팩토리
 */
class LoadingDialogFactory @Inject constructor() {
    /**
     * FragmentManager를 받아 LoadingDialog 인스턴스 생성
     * @param fragmentManager Fragment의 FragmentManager
     * @return LoadingDialog 인스턴스
     */
    fun create(fragmentManager: FragmentManager): LoadingDialog {
        return LoadingDialogImpl(fragmentManager)
    }
}
