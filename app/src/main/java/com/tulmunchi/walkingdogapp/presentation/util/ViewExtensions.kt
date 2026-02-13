package com.tulmunchi.walkingdogapp.presentation.util

import android.view.View

/**
 * 더블 클릭을 방지하는 클릭 리스너
 * @param debounceTime 중복 클릭 방지 시간(밀리초), 기본값 500ms
 * @param action 클릭 시 실행할 동작
 */
fun View.setOnSingleClickListener(
    debounceTime: Long = 500L,
    action: (View) -> Unit
) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime = 0L

        override fun onClick(v: View) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceTime) {
                lastClickTime = currentTime
                action(v)
            }
        }
    })
}
