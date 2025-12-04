package com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.walkInfoWithCalendarPage

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
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.common.HorizonSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.core.components.DayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SaturdayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SelectedMonthDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SundayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.WalkDayDecorator
import com.tulmunchi.walkingdogapp.databinding.FragmentWalkInfoBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.detailWalkInfoPage.DetailWalkInfoFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.collections.get

data class DogsWalkRecord(
    val walkDateList: MutableList<CalendarDay> = mutableListOf(),
    var walkDateInfoFirstSelectedDay: MutableList<WalkRecord> = mutableListOf(),
    var walkDateInfoToday: MutableList<WalkRecord> = mutableListOf(),
    var walkDateInfoList: MutableList<WalkRecord> = mutableListOf()
)

@AndroidEntryPoint
class WalkInfoFragment : Fragment() { // 수정
    private var mainActivity: MainActivity? = null
    private var _binding: FragmentWalkInfoBinding? = null

    private val mainViewModel: MainViewModel by activityViewModels()
    private var selectedDay = CalendarDay.from(2000, 10 ,14)
    private var lateSelectedDay = listOf<String>()

    private var dogsWalkRecordMap = HashMap<String, DogsWalkRecord>()
    private var selectedDog: Dog? = null
    private var previouslySelectedDog: Dog? = null

    private lateinit var adapter: WalkDatesListAdapter
    private val binding get() = _binding!!

    @Inject
    lateinit var networkChecker: NetworkChecker

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

        previouslySelectedDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectDog", Dog::class.java)
        } else {
            (arguments?.getSerializable("selectDog")) as Dog
        }

        lateSelectedDay = arguments?.getStringArrayList("selectDateRecord") ?: listOf()

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
        val dayDecorator = context?.let { DayDecorator(it) } ?: return binding.root
        val sundayDecorator = SundayDecorator()
        val saturdayDecorator = SaturdayDecorator()
        var selectedMonthDecorator = SelectedMonthDecorator(selectedDay.month)
        var walkDayDecorator = WalkDayDecorator(listOf())

        setDogsWalkDate()

        binding.apply {
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
            val walkInfoDogListAdapter = WalkInfoDogListAdapter(
                mainViewModel.dogs.value ?: listOf(), MainActivity.dogImageUrls
            ).also {
                it.onItemClickListener = WalkInfoDogListAdapter.OnItemClickListener { selectedDogInfo ->
                    context?.let { ctx ->
                        if (!networkChecker.isNetworkAvailable()) {
                            return@OnItemClickListener
                        }
                    } ?: return@OnItemClickListener

                    selectedDog = selectedDogInfo
                    selectDogInfo = selectedDog
                    selectDogWalkStats = selectedDogInfo.dogWithStats

                    walkcalendar.removeDecorator(walkDayDecorator)
                    walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDogInfo.name]?.walkDateList ?: listOf()) // 산책한 날 표시
                    walkcalendar.addDecorators(walkDayDecorator)

                    val selectedDayWalkRecords = mutableListOf<WalkRecord>()

                    for(walkRecord in dogsWalkRecordMap[selectedDogInfo.name]?.walkDateInfoList ?: listOf()) {
                        if (walkRecord.day == selectedDay.date.toString()) {
                            selectedDayWalkRecords.add(walkRecord)
                        }
                    }

                    adapter = WalkDatesListAdapter(selectedDayWalkRecords)
                    adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                        goDetail(selectDate)
                    }
                    binding.walkinfoRecyclerview.adapter = adapter
                }
            }

            val spacingItemDecoration = context?.let { ctx ->
                HorizonSpacingItemDecoration(
                    mainViewModel.dogs.value?.size ?: 0,
                    UiUtils.dpToPx(10f, ctx)
                )
            } ?: HorizonSpacingItemDecoration(0, 0) // 리사이클러뷰 아이템 간격

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
            walkcalendar.state().edit().setMaximumDate(CalendarDay.today()).setMinimumDate(CalendarDay.from(2024, 1, 1)).commit()
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
                if (selectedDog == null) {
                    selectedDog = previouslySelectedDog ?: mainViewModel.dogs.value?.firstOrNull()
                    selectDogInfo = selectedDog
                    selectDogWalkStats = selectedDog?.dogWithStats ?: WalkStats()
                }

                viewmodel = mainViewModel
                lifecycleOwner = viewLifecycleOwner

                walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDog?.name]?.walkDateList ?: listOf()) // 산책한 날 표시
                adapter = WalkDatesListAdapter(
                    dogsWalkRecordMap[selectedDog?.name]?.walkDateInfoFirstSelectedDay ?: listOf()
                )
                adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                    goDetail(selectDate)
                }
                walkinfoRecyclerview.adapter = adapter
                walkcalendar.addDecorators(walkDayDecorator)

                walkcalendar.setOnDateChangedListener { _, date, _ -> // 날짜 킅릭시
                    selectedDay = date
                    val walkDateInfoOfDate = mutableListOf<WalkRecord>()
                    for (walkDay: WalkRecord in dogsWalkRecordMap[selectedDog?.name]?.walkDateInfoList ?: listOf()) { // 선택한 날짜의 산책 정보
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
                    selectedDay = walkcalendar.selectedDate ?: CalendarDay.today()
                    adapter = WalkDatesListAdapter(
                        dogsWalkRecordMap[selectedDog?.name]?.walkDateInfoToday ?: listOf()
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
                    walkDayDecorator = WalkDayDecorator(dogsWalkRecordMap[selectedDog?.name]?.walkDateList ?: listOf()) // 산책한 날 표시
                    walkcalendar.addDecorators(walkDayDecorator)
                }
                walkcalendar.addDecorators(dayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator) //데코 설정
            }
            walkinfoRecyclerview.layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun goMyPage() {
        mainActivity?.changeFragment(MyPageFragment())
    }

    private fun setDogsWalkDate() {
        for (dog in MainActivity.dogNameList) {
            dogsWalkRecordMap[dog] = DogsWalkRecord()

            val walkDateInfoFirstSelectedDay = mutableListOf<WalkRecord>()
            val walkDateInfoToday = mutableListOf<WalkRecord>()
            for (walkRecord: WalkRecord in mainViewModel.walkHistory.value?.get(dog) ?: mutableListOf()) {
                val dayInfo = walkRecord.day.split("-")
                if (selectedDay.date.toString() == walkRecord.day) {
                    walkDateInfoFirstSelectedDay.add(walkRecord)
                }

                if(CalendarDay.today().date.toString() == walkRecord.day) {
                    walkDateInfoToday.add(walkRecord)
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
                dogsWalkRecordMap[dog]?.walkDateInfoList?.add(walkRecord)
            }
        }

    }

    private fun goDetail(walkRecord: WalkRecord) {
        val bundle = Bundle()
        bundle.putParcelable("selectDateRecord", walkRecord)
        bundle.putSerializable("selectDog", selectedDog)
        val detailWalkInfoFragment = DetailWalkInfoFragment().apply {
            arguments = bundle
        }
        mainActivity?.changeFragment(detailWalkInfoFragment)
        mainActivity?.setMenuVisibility(View.GONE)
    }

    object DogImgBindingAdapter {
        @BindingAdapter("selectedDog", "viewModel")
        @JvmStatic
        fun loadImage(iv: ImageView, selectedDog: Dog?, viewModel: MainViewModel) {
            if (iv.context == null) return

            try {
                val dogImage = MainActivity.dogImageUrls[selectedDog?.name ?: ""]
                if (dogImage != null) {
                    Glide.with(iv.context).load(dogImage)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(500, 500)
                        .error(R.drawable.collection_003)
                        .into(iv)
                } else {
                    Glide.with(iv.context).load(R.drawable.collection_003)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(500, 500)
                        .into(iv)
                }
            } catch (e: Exception) {
                iv.setImageResource(R.drawable.collection_003)
            }
        }
    }
}