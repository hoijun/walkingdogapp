package com.example.walkingdogapp.album

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.DateDialogBinding
import com.example.walkingdogapp.deco.SelectedMonthDecorator
import com.example.walkingdogapp.deco.ToDayDecorator
import com.example.walkingdogapp.deco.WalkDayDecorator
import com.example.walkingdogapp.userinfo.Walkdate
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter

class DateDialog(context: Context, private val walkdatelist: List<Walkdate>, private val callback : (String) -> Unit) : Dialog(context) {
    private lateinit var binding: DateDialogBinding
    private var walkdates = mutableListOf<CalendarDay>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DateDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val todayDecorator = ToDayDecorator(context, CalendarDay.today())
        var selectedMonthDecorator = SelectedMonthDecorator(CalendarDay.today().month)
        for (date: Walkdate in walkdatelist) {
            val dayinfo = date.day.split("-")
            walkdates.add(
                CalendarDay.from(
                    dayinfo[0].toInt(),
                    dayinfo[1].toInt(),
                    dayinfo[2].toInt()
                )
            ) // 산책한 날 얻음
        }
        val walkDayDecorator = WalkDayDecorator(walkdates) // 산책한 날 표시
        binding.apply {
            walkcalendar.addDecorators(walkDayDecorator, selectedMonthDecorator, todayDecorator)
            walkcalendar.setTitleFormatter { day -> // 년 월 표시 변경
                val inputText = day.date
                val calendarHeaderElements = inputText.toString().split("-").toMutableList()
                val calendarHeaderBuilder = StringBuilder()
                if (calendarHeaderElements[1][0] == '0') {
                    calendarHeaderElements[1] = calendarHeaderElements[1].replace("0","")
                }
                calendarHeaderBuilder.append(calendarHeaderElements[0]).append("년 ")
                    .append(calendarHeaderElements[1]).append("월")
                calendarHeaderBuilder.toString()
            }
            walkcalendar.setWeekDayFormatter(ArrayWeekDayFormatter(context.resources.getTextArray(R.array.custom_weekdays)))
            walkcalendar.state().edit().setMaximumDate(CalendarDay.today()).commit() // 최대 날짜 설정

            walkcalendar.selectedDate = CalendarDay.today() // 오늘 날짜
            
            walkcalendar.setOnDateChangedListener { widget, date, selected ->
                callback(date.date.toString())
                dismiss()
                return@setOnDateChangedListener
            }

            walkcalendar.setOnMonthChangedListener { widget, date -> // 달 바꿀때
                walkcalendar.removeDecorators()
                walkcalendar.invalidateDecorators() // 데코 초기화
                if (date.month == CalendarDay.today().month) {
                    walkcalendar.selectedDate = CalendarDay.today() // 현재 달로 바꿀 때 마다 현재 날짜 표시
                } else {
                    walkcalendar.selectedDate = null
                }
                selectedMonthDecorator = SelectedMonthDecorator(date.month)
                walkcalendar.addDecorators(walkDayDecorator, selectedMonthDecorator, todayDecorator) //데코 설정
            }
        }

        setCancelable(true)
        resizeDialog()
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        window?.attributes = params as WindowManager.LayoutParams
    }
}