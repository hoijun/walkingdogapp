package com.example.walkingdogapp.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.deco.DayDecorator
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.deco.SaturdayDecorator
import com.example.walkingdogapp.deco.SelectedMonthDecorator
import com.example.walkingdogapp.deco.SundayDecorator
import com.example.walkingdogapp.deco.WalkDayDecorator
import com.example.walkingdogapp.userinfo.Walkdate
import com.example.walkingdogapp.databinding.FragmentWalkInfoBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter

// 매개 변수는 상세정보 프래그먼트로 부터 되돌아 올 때 상세정보를 보기 원했던 산책 날짜가 달력에 표시가 유지 되도록 하기 위함
class WalkInfoFragment(private val selectedDayInfo: List<String>) : Fragment() {
    private lateinit var mainactivity: MainActivity
    private var _binding: FragmentWalkInfoBinding? = null

    private val myViewModel: userInfoViewModel by activityViewModels()

    private var walkdates = mutableListOf<CalendarDay>()
    private val walkinfostartday = mutableListOf<Walkdate>()
    private var selectedDay = CalendarDay.from(2000, 10 ,14)

    private lateinit var adapter: WalkdateslistAdapater
    private val binding get() = _binding!!

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMypage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = activity as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        if (selectedDayInfo.isNotEmpty()) {
            selectedDay = CalendarDay.from(
                selectedDayInfo[0].toInt(),
                selectedDayInfo[1].toInt(),
                selectedDayInfo[2].toInt()
            )
        }
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

        binding.apply {
            btnGoMypage.setOnClickListener {
                goMypage()
            }

            if(selectedDayInfo.isEmpty()) { // 마이 페이지에서 들어 왔을 때
                walkcalendar.selectedDate = CalendarDay.today() // 현재 날짜 표시
            } else { // 상세정보 창에서 다시 되돌아 왔을 때
                walkcalendar.selectedDate = selectedDay
            }

            // 산책 정보의 날짜 및 현재 날짜 산책 정보
            for (date: Walkdate in myViewModel.walkDates.value ?: listOf<Walkdate>()) {
                val dayinfo = date.day.split("-")
                walkdates.add(CalendarDay.from(dayinfo[0].toInt(), dayinfo[1].toInt(), dayinfo[2].toInt())) // 산책한 날 얻음

                if(CalendarDay.today().date.toString() == date.day && selectedDayInfo.isEmpty()) { // 마이 페이지에서 들어 왔을 때
                    walkinfostartday.add(date)
                } else if(selectedDay.date.toString() == date.day) {  // 현재 날짜 표시
                    walkinfostartday.add(date)
                }
            }
            val walkDayDecorator = WalkDayDecorator(walkdates) // 산책한 날 표시
            adapter = WalkdateslistAdapater(walkinfostartday)
            adapter.itemClickListener = WalkdateslistAdapater.OnItemClickListener { selectDate ->
                goDetail(selectDate)
            }
            walkinfoRecyclerview.adapter = adapter

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
                for (walkday: Walkdate in  myViewModel.walkDates.value ?: listOf<Walkdate>()) { // 선택한 날짜의 산책 정보
                    if(date.date.toString() == walkday.day) {
                        walkinfoOfdate.add(walkday)
                    }
                }

                adapter = WalkdateslistAdapater(walkinfoOfdate)
                adapter.itemClickListener = WalkdateslistAdapater.OnItemClickListener { selectDate ->
                    goDetail(selectDate)
                }
                walkinfoRecyclerview.adapter = adapter
            }

            walkcalendar.setOnMonthChangedListener { widget, date -> // 달 바꿀때
                walkcalendar.removeDecorators()
                walkcalendar.invalidateDecorators() // 데코 초기화
                if (date.month == CalendarDay.today().month) {
                    walkcalendar.selectedDate = CalendarDay.today() // 현재 달로 바꿀 때 마다 현재 날짜 표시
                    adapter = WalkdateslistAdapater(walkinfostartday)
                } else {
                    adapter = WalkdateslistAdapater(listOf()) // 빈 리사이클러 뷰
                    walkcalendar.selectedDate = null
                }

                selectedMonthDecorator = SelectedMonthDecorator(date.month)
                adapter.itemClickListener = WalkdateslistAdapater.OnItemClickListener { selectDate ->
                    goDetail(selectDate)
                }
                walkinfoRecyclerview.adapter = adapter
                walkcalendar.addDecorators(dayDecorator, walkDayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator) //데코 설정
            }
            walkinfoRecyclerview.layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }

    private fun goMypage() {
        mainactivity.changeFragment(MyPageFragment())
    }

    private fun goDetail(date: Walkdate) {
        mainactivity.changeFragment(DetailWalkInfoFragment(date))
        mainactivity.binding.menuBn.visibility = View.GONE
    }
}