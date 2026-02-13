package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.AddAlarmUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.DeleteAlarmUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.GetAllAlarmsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.ToggleAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for SettingAlarmFragment
 * Manages alarm list and selection state
 */
@HiltViewModel
class SettingAlarmViewModel @Inject constructor(
    private val getAllAlarmsUseCase: GetAllAlarmsUseCase,
    private val addAlarmUseCase: AddAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase
) : BaseViewModel() {

    private val _alarmList = MutableLiveData<List<Alarm>>(emptyList())
    val alarmList: LiveData<List<Alarm>> get() = _alarmList

    private val _removeAlarmList = MutableLiveData<List<Alarm>>(emptyList())

    private val _selectMode = MutableLiveData(false)
    val selectMode: LiveData<Boolean> get() = _selectMode

    /**
     * Load all alarms from database
     */
    fun loadAlarms() {
        viewModelScope.launch {
            _isLoading.value = true
            getAllAlarmsUseCase().handle(
                onSuccess = { alarms ->
                    _alarmList.value = alarms.sortedBy { alarmTimeToString(it.time).toInt() }
                    _isLoading.value = false
                },
                onError = {
                    _error.postValue("알람 목록을 불러오는데 실패했습니다")
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Add new alarm to database
     */
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            addAlarmUseCase(alarm).handle(
                onSuccess = {
                    val currentList = _alarmList.value.orEmpty().toMutableList()
                    currentList.add(alarm)
                    _alarmList.value = currentList.sortedBy { alarmTimeToString(it.time).toInt() }
                },
                onError = {
                    _error.postValue("알람 추가에 실패했습니다")
                }
            )
        }
    }

    /**
     * Update alarm (delete old and add new)
     */
    fun updateAlarm(oldAlarmCode: Int, newAlarm: Alarm) {
        viewModelScope.launch {
            try {
                // Delete old alarm and add new one sequentially
                deleteAlarmUseCase(oldAlarmCode).getOrThrow()
                addAlarmUseCase(newAlarm).getOrThrow()

                // Update UI
                val currentList = _alarmList.value.orEmpty().toMutableList()
                val index = currentList.indexOfFirst { it.alarmCode == oldAlarmCode }
                if (index != -1) {
                    currentList.removeAt(index)
                    currentList.add(newAlarm)
                    _alarmList.value = currentList.sortedBy { alarmTimeToString(it.time).toInt() }
                }
            } catch (e: Exception) {
                _error.postValue("알람 수정에 실패했습니다")
            }
        }
    }

    /**
     * Delete alarms from database
     */
    fun deleteAlarms(alarmsToRemove: List<Alarm>) {
        viewModelScope.launch {
            try {
                alarmsToRemove.forEach { alarm ->
                    deleteAlarmUseCase(alarm.alarmCode).getOrThrow()
                }

                // Update UI
                val currentList = _alarmList.value.orEmpty().toMutableList()
                currentList.removeAll(alarmsToRemove)
                _alarmList.value = currentList
            } catch (e: Exception) {
                _error.postValue("알람 삭제에 실패했습니다")
            }
        }
    }

    /**
     * Toggle alarm on/off
     * DiffUtil will detect only isEnabled changed and apply partial update with payload
     */
    fun toggleAlarm(alarmCode: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarmCode, isEnabled).handle(
                onSuccess = {
                    // Update LiveData - DiffUtil will handle efficient UI update
                    val currentList = _alarmList.value.orEmpty().toMutableList()
                    val index = currentList.indexOfFirst { it.alarmCode == alarmCode }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(isEnabled = isEnabled)
                        _alarmList.value = currentList
                    }
                },
                onError = {
                    _error.postValue("알람 상태 변경에 실패했습니다")
                }
            )
        }
    }

    /**
     * Toggle alarm selection for removal
     */
    fun toggleAlarmSelection(alarm: Alarm) {
        val currentList = _removeAlarmList.value.orEmpty().toMutableList()

        if (currentList.contains(alarm)) {
            currentList.remove(alarm)
        } else {
            currentList.add(alarm)
        }

        _removeAlarmList.value = currentList
    }

    /**
     * Enter select mode
     */
    fun enterSelectMode() {
        _selectMode.value = true
    }

    /**
     * Exit select mode and clear selections
     */
    fun exitSelectMode() {
        _selectMode.value = false
        _removeAlarmList.value = emptyList()
    }

    /**
     * Get alarms to remove
     */
    fun getAlarmsToRemove(): List<Alarm> {
        return _removeAlarmList.value.orEmpty()
    }

    /**
     * Convert alarm time to sortable string format (HHmm)
     */
    private fun alarmTimeToString(time: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = time
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour.toString() + String.format("%02d", minute)
    }
}
