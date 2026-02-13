package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.core.datastore.UserPreferencesDataStore
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.DeleteAlarmUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.alarm.GetAllAlarmsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.user.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getAllAlarmsUseCase: GetAllAlarmsUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase
) : BaseViewModel() {

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _password = MutableLiveData<String>()

    private val _logoutSuccess = MutableLiveData<Boolean>()
    val logoutSuccess: LiveData<Boolean> get() = _logoutSuccess

    private val _deleteAccountSuccess = MutableLiveData<Boolean>()
    val deleteAccountSuccess: LiveData<Boolean> get() = _deleteAccountSuccess

    init {
        loadCredentials()
    }

    /**
     * Load user credentials from DataStore
     */
    fun loadCredentials() {
        viewModelScope.launch {
            _email.value = userPreferencesDataStore.getEmail().first() ?: ""
            _password.value = userPreferencesDataStore.getPassword().first() ?: ""
        }
    }

    /**
     * Check if user logged in with Naver
     */
    fun isNaverUser(): Boolean {
        return _email.value?.contains("@naver.com") == true
    }

    /**
     * Get current email value
     */
    fun getEmail(): String {
        return _email.value ?: ""
    }

    /**
     * Get current password value
     */
    fun getPassword(): String {
        return _password.value ?: ""
    }

    /**
     * Handle logout success - clear all stored data
     */
    fun onLogoutSuccess() {
        viewModelScope.launch {
            userPreferencesDataStore.clearAll()
            _logoutSuccess.value = true
        }
    }

    /**
     * Delete user account from server
     * @return true if deletion was successful
     */
    suspend fun deleteAccount(): Boolean {
        return try {
            deleteAccountUseCase().isSuccess
        } catch (e: Exception) {
            _error.postValue("계정 삭제에 실패했습니다")
            false
        }
    }

    /**
     * Handle delete account success - clear all stored data
     */
    fun onDeleteAccountSuccess() {
        viewModelScope.launch {
            userPreferencesDataStore.clearAll()
            _deleteAccountSuccess.value = true
        }
    }

    /**
     * Get all alarms from database
     */
    suspend fun getAllAlarms(): List<Alarm> {
        return getAllAlarmsUseCase().getOrElse { emptyList() }
    }

    /**
     * Delete all alarms
     */
    fun deleteAlarms(alarms: List<Alarm>) {
        viewModelScope.launch {
            for (alarm in alarms) {
                deleteAlarmUseCase(alarm.alarmCode)
            }
        }
    }

    /**
     * Clear logout success state
     */
    fun clearLogoutSuccess() {
        _logoutSuccess.value = false
    }

    /**
     * Clear delete account success state
     */
    fun clearDeleteAccountSuccess() {
        _deleteAccountSuccess.value = false
    }
}
