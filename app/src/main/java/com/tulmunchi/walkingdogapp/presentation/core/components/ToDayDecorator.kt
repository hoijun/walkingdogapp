package com.tulmunchi.walkingdogapp.presentation.core.components

import android.content.Context
import androidx.core.content.ContextCompat
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.tulmunchi.walkingdogapp.R

class ToDayDecorator(context: Context, private val today: CalendarDay): DayViewDecorator {
    var drawable = ContextCompat.getDrawable(context, R.drawable.custom_transparent_calendar_element)
    // true를 리턴 시 모든 요일에 내가 설정한 드로어블이 적용된다
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == today
    }

    // 일자 선택 시 내가 정의한 드로어블이 적용되도록 한다
    override fun decorate(view: DayViewFacade) {
        if (drawable == null) return
        view.setSelectionDrawable(drawable!!)
    }
}
