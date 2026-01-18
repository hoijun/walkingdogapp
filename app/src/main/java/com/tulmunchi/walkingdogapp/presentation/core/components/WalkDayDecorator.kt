package com.tulmunchi.walkingdogapp.presentation.core.components

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan

// 산책 날짜에 점 표시
class WalkDayDecorator(private val walkDays : List<CalendarDay>): DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        return walkDays.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(6F, Color.BLACK))
    }
}
