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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.common.HorizonSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentWalkInfoBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.core.components.DayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SaturdayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SelectedMonthDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.SundayDecorator
import com.tulmunchi.walkingdogapp.presentation.core.components.WalkDayDecorator
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.WalkInfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalkInfoFragment : Fragment() {
    private var _binding: FragmentWalkInfoBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val walkInfoViewModel: WalkInfoViewModel by viewModels()
    
    private var lateSelectedDay = listOf<String>()
    private var previouslySelectedDog: Dog? = null
    private var walkDayDecorator = WalkDayDecorator(listOf())
    private lateinit var adapter: WalkDatesListAdapter

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var navigationManager: NavigationManager

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateToMyPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previouslySelectedDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectDog", Dog::class.java)
        } else {
            (arguments?.getSerializable("selectDog")) as? Dog
        }

        lateSelectedDay = arguments?.getStringArrayList("selectDateRecord") ?: listOf()

        val initialDay = if (lateSelectedDay.isNotEmpty()) {
            CalendarDay.from(
                lateSelectedDay[0].toInt(),
                lateSelectedDay[1].toInt(),
                lateSelectedDay[2].toInt()
            )
        } else {
            CalendarDay.today()
        }

        // ViewModel 초기화
        val dogs = mainViewModel.dogs.value ?: emptyList()
        val walkHistory = mainViewModel.walkHistory.value ?: emptyMap()
        
        walkInfoViewModel.initializeWithDogs(dogs, walkHistory, initialDay)
        walkInfoViewModel.updateSelectedDog(previouslySelectedDog)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalkInfoBinding.inflate(inflater, container, false)

        setupViewModelObservers()

        binding.apply {
            btnBack.setOnClickListener { navigateToMyPage() }
            
            setupDogRecyclerView()
            setupCalendar()
            setupWalkRecordsRecyclerView()
            
            // DataBinding 변수 초기화
            val dogNames = mainViewModel.dogNames.value ?: emptyList()
            if (dogNames.isNotEmpty()) {
                selectDogInfo = walkInfoViewModel.selectedDog.value
                selectDogWalkStats = walkInfoViewModel.selectedDog.value?.dogWithStats ?: WalkStats()
                viewmodel = mainViewModel
                lifecycleOwner = viewLifecycleOwner
            }
        }
        
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 강아지 선택 RecyclerView 초기화
     */
    private fun setupDogRecyclerView() {
        val walkInfoDogListAdapter = WalkInfoDogListAdapter(
            mainViewModel.dogs.value ?: listOf(), 
            mainViewModel.dogImages.value ?: emptyMap()
        ).also {
            it.onItemClickListener = WalkInfoDogListAdapter.OnItemClickListener { selectedDogInfo ->
                if (!networkChecker.isNetworkAvailable()) return@OnItemClickListener
                
                walkInfoViewModel.selectDog(selectedDogInfo)
                binding.selectDogInfo = selectedDogInfo
                binding.selectDogWalkStats = selectedDogInfo.dogWithStats
            }
        }

        val spacingItemDecoration = context?.let { ctx ->
            HorizonSpacingItemDecoration(UiUtils.dpToPx(10f, ctx))
        } ?: HorizonSpacingItemDecoration(0)

        binding.selectDogsRecyclerView.apply {
            addItemDecoration(spacingItemDecoration)
            adapter = walkInfoDogListAdapter
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
        }
    }

    /**
     * 캘린더 초기화 및 설정
     */
    private fun setupCalendar() {
        val dayDecorator = context?.let { DayDecorator(it) } ?: return
        val sundayDecorator = SundayDecorator()
        val saturdayDecorator = SaturdayDecorator()
        var selectedMonthDecorator = SelectedMonthDecorator(
            walkInfoViewModel.selectedDay.value?.month ?: CalendarDay.today().month
        )

        binding.walkCalendar.apply {
            // 초기 선택 날짜 설정
            walkInfoViewModel.selectedDay.value?.let { selectedDay ->
                selectedDate = selectedDay
            }

            // Decorators 추가
            addDecorators(dayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator)
            
            // 요일 포맷 설정
            setWeekDayFormatter(ArrayWeekDayFormatter(resources.getTextArray(R.array.custom_weekdays)))
            
            // 날짜 범위 설정
            state().edit()
                .setMaximumDate(CalendarDay.today())
                .setMinimumDate(CalendarDay.from(2024, 1, 1))
                .commit()
            
            // 타이틀 포맷 (년 월 표시 변경)
            setTitleFormatter { day ->
                val inputText = day.date
                val calendarHeaderElements = inputText.toString().split("-").toMutableList()
                if (calendarHeaderElements[1][0] == '0') {
                    calendarHeaderElements[1] = calendarHeaderElements[1].replace("0", "")
                }
                "${calendarHeaderElements[0]}년 ${calendarHeaderElements[1]}월"
            }

            // 초기 산책 날짜 Decorator 추가
            val dogNames = mainViewModel.dogNames.value ?: emptyList()
            if (dogNames.isNotEmpty()) {
                walkInfoViewModel.walkDateList.value?.let { dateList ->
                    walkDayDecorator = WalkDayDecorator(dateList)
                    addDecorators(walkDayDecorator)
                }

                // 날짜 선택 리스너
                setOnDateChangedListener { _, date, _ ->
                    walkInfoViewModel.selectDay(date)
                }
            }

            // 월 변경 리스너
            setOnMonthChangedListener { _, date ->
                removeDecorators()
                invalidateDecorators()
                
                // ViewModel을 통해 월 변경 처리
                val walkRecords = walkInfoViewModel.onMonthChanged(date)
                adapter = WalkDatesListAdapter(walkRecords)
                adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { selectDate ->
                    navigateToDetailWalkInfo(selectDate)
                }
                binding.walkInfoRecyclerview.adapter = adapter

                // 현재 월인 경우 오늘 날짜 선택
                selectedDate = if (date.month == CalendarDay.today().month) {
                    CalendarDay.today()
                } else {
                    null
                }

                // Decorator 재설정
                selectedMonthDecorator = SelectedMonthDecorator(date.month)
                if (dogNames.isNotEmpty()) {
                    walkInfoViewModel.walkDateList.value?.let { dateList ->
                        walkDayDecorator = WalkDayDecorator(dateList)
                        addDecorators(walkDayDecorator)
                    }
                }
                addDecorators(dayDecorator, saturdayDecorator, sundayDecorator, selectedMonthDecorator)
            }
        }
    }

    /**
     * 산책 기록 RecyclerView 초기화
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupWalkRecordsRecyclerView() {
        // 스크롤 뷰와 리사이클러뷰 스크롤 겹침 방지
        binding.walkInfoRecyclerviewLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    binding.WalkInfoScrollView.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    binding.WalkInfoScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_DOWN -> {
                    binding.WalkInfoScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }

        binding.walkInfoRecyclerview.layoutManager = LinearLayoutManager(context)
    }

    /**
     * ViewModel 관찰자 설정
     */
    private fun setupViewModelObservers() {
        // 선택된 날짜의 산책 기록 관찰
        walkInfoViewModel.selectedDayWalkRecords.observe(viewLifecycleOwner) { walkRecords ->
            adapter = WalkDatesListAdapter(walkRecords)
            adapter.itemClickListener = WalkDatesListAdapter.OnItemClickListener { walkRecord ->
                navigateToDetailWalkInfo(walkRecord)
            }
            binding.walkInfoRecyclerview.adapter = adapter
        }

        // 산책한 날짜 리스트 관찰 (캘린더 데코레이터 업데이트)
        walkInfoViewModel.walkDateList.observe(viewLifecycleOwner) { dateList ->
            binding.walkCalendar.removeDecorator(walkDayDecorator)
            walkDayDecorator = WalkDayDecorator(dateList)
            binding.walkCalendar.addDecorators(walkDayDecorator)
        }

        // 선택된 강아지 관찰
        walkInfoViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            binding.selectDogInfo = dog
            binding.selectDogWalkStats = dog?.dogWithStats ?: WalkStats()
        }
    }

    private fun navigateToMyPage() {
        navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
    }

    private fun navigateToDetailWalkInfo(walkRecord: WalkRecord) {
        navigationManager.navigateTo(
            NavigationState.WithoutBottomNav.DetailWalkInfo(
                walkRecord,
                walkInfoViewModel.selectedDog.value ?: Dog("", "", "", "", "", "", "", "", 0L)
            )
        )
    }

    object DogImgBindingAdapter {
        @BindingAdapter("selectedDog", "viewModel")
        @JvmStatic
        fun loadImage(iv: ImageView, selectedDog: Dog?, viewModel: MainViewModel?) {
            if (iv.context == null) return
            if (selectedDog == null) return
            if (viewModel == null) return

            try {
                val dogImage = viewModel.dogImages.value?.get(selectedDog.name ?: "")
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