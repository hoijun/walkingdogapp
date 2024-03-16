package com.example.walkingdogapp.deco

import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.example.walkingdogapp.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import org.threeten.bp.DayOfWeek

class DayDecorator(context: Context): DayViewDecorator {
    var drawable = ContextCompat.getDrawable(context, R.drawable.calendar_selector)
    // true를 리턴 시 모든 요일에 내가 설정한 드로어블이 적용된다
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return true
    }

    // 일자 선택 시 내가 정의한 드로어블이 적용되도록 한다
    override fun decorate(view: DayViewFacade) {
        view.setSelectionDrawable(drawable!!)
    }
}

class ToDayDecorator(context: Context, private val today: CalendarDay): DayViewDecorator {
    var drawable = ContextCompat.getDrawable(context, R.drawable.transparent_calendar_element)
    // true를 리턴 시 모든 요일에 내가 설정한 드로어블이 적용된다
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == today
    }

    // 일자 선택 시 내가 정의한 드로어블이 적용되도록 한다
    override fun decorate(view: DayViewFacade) {
        view.setSelectionDrawable(drawable!!)
    }
}

// 선택한 달 외의 날짜 표시
class SelectedMonthDecorator(private val selectedMonth : Int): DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.month != selectedMonth
    }
    override fun decorate(view: DayViewFacade) {
        view.addSpan(ForegroundColorSpan(Color.GRAY))
    }
}


// 산책 날짜에 점 표시
class WalkDayDecorator(private val walkdays : List<CalendarDay>): DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        return walkdays.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(12F, Color.parseColor("#FFA500")))
    }
}

class SundayDecorator: DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        val sunday = day.date.with(DayOfWeek.SUNDAY).dayOfMonth
        return sunday == day.day
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(object:ForegroundColorSpan(Color.RED){})
    }
}

/* 토요일 날짜의 색상을 설정하는 클래스 */
class SaturdayDecorator: DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        val saturday = day.date.with(DayOfWeek.SATURDAY).dayOfMonth
        return saturday == day.day
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(object:ForegroundColorSpan(Color.BLUE){})
    }
}