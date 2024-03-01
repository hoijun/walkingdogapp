package com.example.walkingdogapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.databinding.FragmentWalkInfoBinding
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter


class WalkInfoFragment : Fragment() {
    private lateinit var mainactivity: MainActivity
    private var _binding: FragmentWalkInfoBinding? = null
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var userdogInfo: DogInfo
    private var walkdates = mutableListOf<CalendarDay>()
    private lateinit var adapter: WalkdateslistAdapater
    private val binding get() = _binding!!
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            mainactivity.changeFragment(MyPageFragment())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = activity as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        userdogInfo = myViewModel.doginfo.value ?: DogInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalkInfoBinding.inflate(inflater,container, false)

        // 달력 커스텀
        val dayDecorator = DayDecorator(requireContext())
        val sundayDecorator = SundayDecorator()
        val saturdayDecorator = SaturdayDecorator()
        var selectedMonthDecorator = SelectedMonthDecorator(CalendarDay.today().month)
        val walkinfotoday = mutableListOf<Walkdate>()

        // 산책 정보의 날짜 및 현재 날짜 산책 정보
        for (date: Walkdate in userdogInfo.dates) {
            val dayinfo = date.day.split("-")
            walkdates.add(CalendarDay.from(dayinfo[0].toInt(), dayinfo[1].toInt(), dayinfo[2].toInt()))

            if(CalendarDay.today().date.toString() == date.day) {
                walkinfotoday.add(date)
            }
        }

        val walkDayDecorator = WalkDayDecorator(walkdates)

        binding.apply {
            adapter = WalkdateslistAdapater(walkinfotoday)
            walkinfoRecyclerview.adapter = adapter

            walkcalendar.selectedDate = CalendarDay.today() // 현재 날짜 표시
            walkcalendar.addDecorators(dayDecorator, walkDayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator)
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
            walkcalendar.setWeekDayFormatter(ArrayWeekDayFormatter(resources.getTextArray(R.array.custom_weekdays)))
            walkcalendar.state().edit().setMaximumDate(CalendarDay.today()).commit() // 최대 날짜 설정

            walkcalendar.setOnDateChangedListener { widget, date, selected -> // 날짜 킅릭시
                val walkinfoOfdate = mutableListOf<Walkdate>()
                for (walkday: Walkdate in userdogInfo.dates) { // 선택한 날짜의 산책 정보
                    if(date.date.toString() == walkday.day) {
                        walkinfoOfdate.add(walkday)
                    }
                }

                adapter = WalkdateslistAdapater(walkinfoOfdate)
                walkinfoRecyclerview.adapter = adapter
            }
            walkcalendar.setOnMonthChangedListener { widget, date -> // 달 바꿀때
                walkcalendar.removeDecorators()
                walkcalendar.invalidateDecorators() // 데코 초기화
                adapter = WalkdateslistAdapater(listOf()) // 빈 리사이클러 뷰

                if (date.month == CalendarDay.today().month) {
                    walkcalendar.selectedDate = CalendarDay.today() // 현재 달로 바꿀 때 마다 현재 날짜 표시
                } else {
                    walkcalendar.selectedDate = null
                }

                selectedMonthDecorator = SelectedMonthDecorator(date.month) 
                walkinfoRecyclerview.adapter = adapter
                walkcalendar.addDecorators(dayDecorator, walkDayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator) //데코 설정
            }

            walkinfoRecyclerview.layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }
}