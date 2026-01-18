package com.tulmunchi.walkingdogapp.presentation.core.components

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import org.threeten.bp.DayOfWeek

/* 토요일 날짜의 색상을 설정하는 클래스 */
class SaturdayDecorator: DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        val saturday = day.date.with(DayOfWeek.SATURDAY).dayOfMonth
        return saturday == day.day
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(object: ForegroundColorSpan(Color.BLUE){})
    }
}
