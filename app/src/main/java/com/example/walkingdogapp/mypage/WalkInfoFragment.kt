package com.example.walkingdogapp.mypage

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.utils.utils.Utils
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.utils.utils.NetworkManager
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentWalkInfoBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.WalkDateInfo
import com.example.walkingdogapp.utils.utils.DayDecorator
import com.example.walkingdogapp.utils.HorizonSpacingItemDecoration
import com.example.walkingdogapp.utils.utils.SaturdayDecorator
import com.example.walkingdogapp.utils.utils.SelectedMonthDecorator
import com.example.walkingdogapp.utils.utils.SundayDecorator
import com.example.walkingdogapp.utils.utils.WalkDayDecorator
import com.example.walkingdogapp.viewmodel.MainViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter

data class DogsWalkRecord(
    val walkDateList: MutableList<CalendarDay> = mutableListOf(),
    var walkDateInfoFirstSelectedDay: MutableList<WalkDateInfo> = mutableListOf(),
    var walkDateInfoToday: MutableList<WalkDateInfo> = mutableListOf(),
    var walkDateInfoList: MutableList<WalkDateInfo> = mutableListOf()
)

class WalkInfoFragment : Fragment() { // 수정
    private lateinit var mainActivity: MainActivity
    private var _binding: FragmentWalkInfoBinding? = null

    private val mainViewModel: MainViewModel by activityViewModels()
    private var selectedDay = CalendarDay.from(2000, 10 ,14)
    private var lateSelectedDay = listOf<String>()

    private var dogsWalkRecordMap = HashMap<String, DogsWalkRecord>()
    private var selectedDog = DogInfo()

    private lateinit var adapter: WalkDatesListAdapter
    private val binding get() = _binding!!

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        mainActivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        selectedDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectDog", DogInfo::class.java)?: DogInfo()
        } else {
            (arguments?.getSerializable("selectDog") ?: DogInfo()) as DogInfo
        }

        lateSelectedDay = arguments?.getStringArrayList("selectDateRecord") ?: listOf<String>()

        selectedDay = if (lateSelectedDay.isNotEmpty()) {
            CalendarDay.from(
                lateSelectedDay[0].toInt(),
                lateSelectedDay[1].toInt(),
                lateSelectedDay[2].toInt()
            )
        } else {
            CalendarDay.today()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalkInfoBinding.inflate(inflater,container, false)

        // 달력 커스텀
        val dayDecorator = DayDecorator(requireContext())
        val sundayDecorator = SundayDecorator()
        val saturdayDecorator = SaturdayDecorator()
        var selectedMonthDecorator = SelectedMonthDecorator(selectedDay.month)
        var walkDayDecorator = WalkDayDecorator(listOf())

        setDogsWalkDate()

        binding.apply {
            selectDogInfo = selectedDog
            btnGoMypage.setOnClickListener {
                goMyPage()
            }

            walkinfoRecyclerviewLayout.setOnTouchListener { _, event -> // 리사이클러 뷰, 스크롤 뷰 스크롤 겹침 방지
                when(event.action) {
                    MotionEvent.ACTION_UP -> {
                        WalkInfoScrollView.requestDisallowInterceptTouchEvent(false)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        WalkInfoScrollView.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    MotionEvent.ACTION_DOWN -> {
                        WalkInfoScrollView.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    else -> true
                }
            }

            val walkInfoDogListAdapter = WalkInfoDogListAdapter(requireContext(), mainViewModel).also {
                it.onItemClickListener = WalkInfoDogListAdapter.OnItemClickListener { selectedDogInfo ->
                    if(!NetworkManager.checkNetworkState(requireContext())) {
                        return@OnItemClickListener
                    }
                    selectedDog = selectedDogInfo
                    selectDogInfo = selectedDog

                    walkcalendar.removeDecorator(walkDayDecorator)
                    walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDogInfo.name]!!.walkDateList) // 산책한 날 표시
                    walkcalendar.addDecorators(walkDayDecorator)

                    val selectedDayWalkDateInfoList = mutableListOf<WalkDateInfo>()

                    for(walkRecord in dogsWalkRecordMap[selectedDogInfo.name]?.walkDateInfoList ?: listOf()) {
                        if (walkRecord.day == selectedDay.date.toString()) {
                            selectedDayWalkDateInfoList.add(walkRecord)
                        }
                    }

                    adapter = WalkDatesListAdapter(selectedDayWalkDateInfoList)
                    adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                        goDetail(selectDate)
                    }
                    binding.walkinfoRecyclerview.adapter = adapter
                }
            }

            val spacingItemDecoration = HorizonSpacingItemDecoration(mainViewModel.dogsInfo.value?.size ?: 0, Utils.dpToPx(10f, requireContext())) // 리사이클러뷰 아이템 간격
            selectDogsRecyclerView.addItemDecoration(spacingItemDecoration)
            selectDogsRecyclerView.adapter = walkInfoDogListAdapter
            selectDogsRecyclerView.layoutManager = LinearLayoutManager(context).apply {
                this.orientation = LinearLayoutManager.HORIZONTAL
            }


            if (lateSelectedDay.isEmpty()) { // 마이 페이지에서 들어 왔을 때
                walkcalendar.selectedDate = selectedDay // 현재 날짜 표시
            } else { // 상세정보 창에서 다시 되돌아 왔을 때
                walkcalendar.selectedDate = selectedDay
            }

            walkcalendar.addDecorators(dayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator)
            walkcalendar.setWeekDayFormatter(ArrayWeekDayFormatter(resources.getTextArray(R.array.custom_weekdays)))
            walkcalendar.state().edit().setMaximumDate(CalendarDay.today()).commit() // 최대 날짜 설정
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

            if (MainActivity.dogNameList.isNotEmpty()) {
                if (selectedDog == DogInfo()) {
                    selectedDog = mainViewModel.dogsInfo.value!![0]
                    selectDogInfo = selectedDog
                }

                selectDogInfo = selectedDog
                viewmodel = mainViewModel
                lifecycleOwner = requireActivity()

                walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDog.name]!!.walkDateList) // 산책한 날 표시
                adapter = WalkDatesListAdapter(dogsWalkRecordMap[selectedDog.name]!!.walkDateInfoFirstSelectedDay)
                adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                    goDetail(selectDate)
                }
                walkinfoRecyclerview.adapter = adapter
                walkcalendar.addDecorators(walkDayDecorator)

                walkcalendar.setOnDateChangedListener { _, date, _ -> // 날짜 킅릭시
                    selectedDay = date
                    val walkDateInfoOfDate = mutableListOf<WalkDateInfo>()
                    for (walkDay: WalkDateInfo in dogsWalkRecordMap[selectedDog.name]?.walkDateInfoList ?: listOf()) { // 선택한 날짜의 산책 정보
                        if(date.date.toString() == walkDay.day) {
                            walkDateInfoOfDate.add(walkDay)
                        }
                    }

                    adapter = WalkDatesListAdapter(walkDateInfoOfDate)
                    adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                        goDetail(selectDate)
                    }
                    walkinfoRecyclerview.adapter = adapter
                }
            }


            walkcalendar.setOnMonthChangedListener { _, date -> // 달 바꿀때
                walkcalendar.removeDecorators()
                walkcalendar.invalidateDecorators() // 데코 초기화
                if (date.month == CalendarDay.today().month) {
                    walkcalendar.selectedDate = CalendarDay.today() // 현재 달로 바꿀 때 마다 현재 날짜 표시
                    selectedDay = walkcalendar.selectedDate!!
                    adapter = WalkDatesListAdapter(
                        dogsWalkRecordMap[selectedDog.name]?.walkDateInfoToday ?: listOf()
                    )
                } else {
                    adapter = WalkDatesListAdapter(listOf()) // 빈 리사이클러 뷰
                    walkcalendar.selectedDate = null
                }

                selectedMonthDecorator = SelectedMonthDecorator(date.month)
                adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                    goDetail(selectDate)
                }
                walkinfoRecyclerview.adapter = adapter

                if (MainActivity.dogNameList.isNotEmpty()) {
                    walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDog.name]!!.walkDateList) // 산책한 날 표시
                    walkcalendar.addDecorators(walkDayDecorator)
                }
                walkcalendar.addDecorators(dayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator) //데코 설정
            }
            walkinfoRecyclerview.layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goMyPage() {
        mainActivity.changeFragment(MyPageFragment())
    }

    private fun setDogsWalkDate() {
        for (dog in MainActivity.dogNameList) {
            dogsWalkRecordMap[dog] = DogsWalkRecord()

            val walkDateInfoFirstSelectedDay = mutableListOf<WalkDateInfo>()
            val walkDateInfoToday = mutableListOf<WalkDateInfo>()
            for (walkDateInfo: WalkDateInfo in mainViewModel.walkDates.value?.get(dog)
                ?: mutableListOf()) {
                val dayInfo = walkDateInfo.day.split("-")
                if (selectedDay.date.toString() == walkDateInfo.day) {
                    walkDateInfoFirstSelectedDay.add(walkDateInfo)
                }

                if(CalendarDay.today().date.toString() == walkDateInfo.day) {
                    walkDateInfoToday.add(walkDateInfo)
                }

                dogsWalkRecordMap[dog]?.walkDateList?.add(
                    CalendarDay.from(
                        dayInfo[0].toInt(),
                        dayInfo[1].toInt(),
                        dayInfo[2].toInt()
                    )
                ) // 산책한 날 얻음

                dogsWalkRecordMap[dog]?.walkDateInfoFirstSelectedDay = walkDateInfoFirstSelectedDay
                dogsWalkRecordMap[dog]?.walkDateInfoToday = walkDateInfoToday
                dogsWalkRecordMap[dog]?.walkDateInfoList?.add(walkDateInfo)
            }
        }

    }

    private fun goDetail(walkDateInfo: WalkDateInfo) {
        val bundle = Bundle()
        bundle.putSerializable("selectDateRecord", walkDateInfo)
        bundle.putSerializable("selectDog", selectedDog)
        val detailWalkInfoFragment = DetailWalkInfoFragment().apply {
            arguments = bundle
        }
        mainActivity.changeFragment(detailWalkInfoFragment)
        mainActivity.binding.menuBn.visibility = View.GONE
    }

    object DogImgBindingAdapter {
        @BindingAdapter("selectedDog", "viewModel")
        @JvmStatic
        fun loadImage(iv: ImageView, selectedDog: DogInfo, viewModel: MainViewModel) {
            if (viewModel.dogsImg.value?.get(selectedDog.name) != null) {
                Glide.with(iv.context).load(viewModel.dogsImg.value?.get(selectedDog.name))
                    .format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(iv)
            } else {
                Glide.with(iv.context).load(R.drawable.collection_003)
                    .format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(iv)
            }
        }
    }
}