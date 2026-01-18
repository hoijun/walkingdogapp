package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 강아지별 산책 기록 데이터 클래스
 */
data class DogsWalkRecord(
    val walkDateList: MutableSet<CalendarDay> = mutableSetOf(),
    var walkDateInfoFirstSelectedDay: MutableList<WalkRecord> = mutableListOf(),
    var walkDateInfoToday: MutableList<WalkRecord> = mutableListOf(),
    var walkDateInfoList: MutableList<WalkRecord> = mutableListOf()
)

/**
 * ViewModel for WalkInfo screen
 * Manages walk records, selected dog, and calendar state
 */
@HiltViewModel
class WalkInfoViewModel @Inject constructor() : BaseViewModel() {

    // 선택된 강아지
    private val _selectedDog = MutableLiveData<Dog?>()
    val selectedDog: LiveData<Dog?> get() = _selectedDog

    // 선택된 날짜
    private val _selectedDay = MutableLiveData<CalendarDay?>()
    val selectedDay: LiveData<CalendarDay?> get() = _selectedDay

    // 강아지별 산책 기록 맵
    private val _dogsWalkRecordMap = MutableLiveData<HashMap<String, DogsWalkRecord>>()

    // 선택된 날짜의 산책 기록 리스트
    private val _selectedDayWalkRecords = MutableLiveData<List<WalkRecord>>()
    val selectedDayWalkRecords: LiveData<List<WalkRecord>> get() = _selectedDayWalkRecords

    // 산책한 날짜 리스트 (현재 선택된 강아지 기준)
    private val _walkDateList = MutableLiveData<List<CalendarDay>>()
    val walkDateList: LiveData<List<CalendarDay>> get() = _walkDateList

    /**
     * Initialize ViewModel with dogs and walk history data
     * @param dogs List of dogs
     * @param walkHistory Map of dog name to walk records
     * @param initialDay Initial selected day (default: today or from previous screen)
     */
    fun initializeWithDogs(
        dogs: List<Dog>,
        walkHistory: Map<String, List<WalkRecord>>,
        initialDay: CalendarDay
    ) {
        _selectedDay.value = initialDay
        
        val dogsWalkRecordMap = HashMap<String, DogsWalkRecord>()
        val dogNames = dogs.map { it.name }

        // 각 강아지별로 산책 기록 데이터 구성
        for (dogName in dogNames) {
            dogsWalkRecordMap[dogName] = DogsWalkRecord()

            val walkDateInfoFirstSelectedDay = mutableListOf<WalkRecord>()
            val walkDateInfoToday = mutableListOf<WalkRecord>()

            for (walkRecord in walkHistory[dogName] ?: emptyList()) {
                val dayInfo = walkRecord.day.split("-")

                // 초기 선택 날짜의 산책 기록
                if (initialDay.date.toString() == walkRecord.day) {
                    walkDateInfoFirstSelectedDay.add(walkRecord)
                }

                // 오늘 날짜의 산책 기록
                if (CalendarDay.today().date.toString() == walkRecord.day) {
                    walkDateInfoToday.add(walkRecord)
                }

                // CalendarDay로 변환하여 저장
                dogsWalkRecordMap[dogName]?.walkDateList?.add(
                    CalendarDay.from(
                        dayInfo[0].toInt(),
                        dayInfo[1].toInt(),
                        dayInfo[2].toInt()
                    )
                )

                dogsWalkRecordMap[dogName]?.walkDateInfoFirstSelectedDay = walkDateInfoFirstSelectedDay
                dogsWalkRecordMap[dogName]?.walkDateInfoToday = walkDateInfoToday
                dogsWalkRecordMap[dogName]?.walkDateInfoList?.add(walkRecord)
            }
        }

        _dogsWalkRecordMap.value = dogsWalkRecordMap

        // 첫 번째 강아지를 기본 선택
        if (dogs.isNotEmpty()) {
            val firstDog = dogs.firstOrNull()
            _selectedDog.value = firstDog
            firstDog?.let { dog ->
                _walkDateList.value = dogsWalkRecordMap[dog.name]?.walkDateList?.toList() ?: emptyList()
                _selectedDayWalkRecords.value = dogsWalkRecordMap[dog.name]?.walkDateInfoFirstSelectedDay ?: emptyList()
            }
        }
    }

    /**
     * Update selected dog data from previous screen if exists
     */
    fun updateSelectedDog(dog: Dog?) {
        dog?.let {
            _selectedDog.value = it
            updateWalkDataForSelectedDog()
        }
    }

    /**
     * Select a dog and update walk data accordingly
     */
    fun selectDog(dog: Dog) {
        _selectedDog.value = dog
        updateWalkDataForSelectedDog()
    }

    /**
     * Select a day and update walk records for that day
     */
    fun selectDay(day: CalendarDay) {
        _selectedDay.value = day
        updateWalkRecordsForSelectedDay()
    }

    /**
     * Get walk records for current month change
     */
    fun onMonthChanged(newMonth: CalendarDay): List<WalkRecord> {
        return if (newMonth.month == CalendarDay.today().month) {
            // 현재 달로 돌아왔을 때 오늘 날짜의 산책 기록 반환
            _selectedDay.value = CalendarDay.today()
            val dogName = _selectedDog.value?.name ?: return emptyList()
            _dogsWalkRecordMap.value?.get(dogName)?.walkDateInfoToday ?: emptyList()
        } else {
            // 다른 달로 이동했을 때 빈 리스트 반환
            _selectedDay.value = null
            emptyList()
        }
    }

    /**
     * Update walk data when dog selection changes
     */
    private fun updateWalkDataForSelectedDog() {
        val dogName = _selectedDog.value?.name ?: return
        val recordMap = _dogsWalkRecordMap.value ?: return

        _walkDateList.value = recordMap[dogName]?.walkDateList?.toList() ?: emptyList()
        updateWalkRecordsForSelectedDay()
    }

    /**
     * Update walk records when day selection changes
     */
    private fun updateWalkRecordsForSelectedDay() {
        val dogName = _selectedDog.value?.name ?: return
        val selectedDay = _selectedDay.value ?: return
        val recordMap = _dogsWalkRecordMap.value ?: return

        val walkRecordsForDay = mutableListOf<WalkRecord>()
        for (walkRecord in recordMap[dogName]?.walkDateInfoList ?: emptyList()) {
            if (selectedDay.date.toString() == walkRecord.day) {
                walkRecordsForDay.add(walkRecord)
            }
        }

        _selectedDayWalkRecords.value = walkRecordsForDay
    }
}
