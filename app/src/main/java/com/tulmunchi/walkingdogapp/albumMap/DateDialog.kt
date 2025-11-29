package com.tulmunchi.walkingdogapp.albumMap

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.DateDialogBinding
import com.tulmunchi.walkingdogapp.common.SelectedMonthDecorator
import com.tulmunchi.walkingdogapp.common.ToDayDecorator
import com.tulmunchi.walkingdogapp.common.WalkDayDecorator
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import androidx.core.graphics.drawable.toDrawable

class DateDialog : DialogFragment() {
    private var _binding: DateDialogBinding? = null
    private val binding get() = _binding!!
    private var walkdates = mutableListOf<CalendarDay>()
    private val userDataViewModel: MainViewModel by activityViewModels()

    fun interface OnDateClickListener {
        fun onDateClick(date: String)
    }

    var dateClickListener: OnDateClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val walkRecordList = userDataViewModel.walkDates.value?: hashMapOf()
        for(dog in MainActivity.dogNameList) {
            for(date in walkRecordList[dog] ?: mutableListOf()) {
                val dayInfo = date.day.split("-")
                walkdates.add(
                    CalendarDay.from(
                        dayInfo[0].toInt(),
                        dayInfo[1].toInt(),
                        dayInfo[2].toInt()
                    )
                ) // 산책한 날 얻음
            }
        }

        walkdates = walkdates.toMutableSet().toMutableList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DateDialogBinding.inflate(inflater, container, false)
        val todayDecorator = ToDayDecorator(requireContext(), CalendarDay.today())
        var selectedMonthDecorator = SelectedMonthDecorator(CalendarDay.today().month)
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

            walkcalendar.setWeekDayFormatter(ArrayWeekDayFormatter(requireContext().resources.getTextArray(R.array.custom_weekdays)))
            walkcalendar.state().edit().setMaximumDate(CalendarDay.today()).setMinimumDate(CalendarDay.from(2024, 1, 1)).commit()

            walkcalendar.selectedDate = CalendarDay.today() // 오늘 날짜

            walkcalendar.setOnDateChangedListener { _, date, _ ->
                dateClickListener?.onDateClick(date.date.toString())
                dismiss()
                return@setOnDateChangedListener
            }

            walkcalendar.setOnMonthChangedListener { _, date -> // 달 바꿀때
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

        isCancelable = true
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}