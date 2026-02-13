package com.tulmunchi.walkingdogapp.presentation.core.components

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

// 선택한 달 외의 날짜 표시
class SelectedMonthDecorator(private val selectedMonth : Int): DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.month != selectedMonth
    }
    override fun decorate(view: DayViewFacade) {
        view.addSpan(ForegroundColorSpan(Color.GRAY))
    }
}
