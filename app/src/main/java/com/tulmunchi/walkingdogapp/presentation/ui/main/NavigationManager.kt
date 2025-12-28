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
}
