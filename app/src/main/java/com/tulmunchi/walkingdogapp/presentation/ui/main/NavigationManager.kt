package com.tulmunchi.walkingdogapp.presentation.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() {

    private val _currentState = MutableLiveData<NavigationState>()
    val currentState: LiveData<NavigationState> get() = _currentState

    fun navigateTo(state: NavigationState) {
        _currentState.value = state
    }

    fun navigateBack() {
        // 기본적으로 Home으로 이동
        // 필요시 BackStack 구현 가능
        _currentState.value = NavigationState.WithBottomNav.Home
    }
}
