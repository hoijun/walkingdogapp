package com.tulmunchi.walkingdogapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserInfoRepository
): ViewModel() {
    private val _successSignUp = MutableLiveData<Boolean>()
    val successSignUp: MutableLiveData<Boolean>
        get() = _successSignUp

    fun setUser() {
        repository.setUser()
    }

    fun signUp(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            setUser()
            repository.signUp(email, _successSignUp)
        }
    }
}