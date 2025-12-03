package com.tulmunchi.walkingdogapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.usecase.user.SignUpUseCase
import com.tulmunchi.walkingdogapp.presentation.ui.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : BaseViewModel() {

    private val _signUpSuccess = MutableLiveData<Boolean>()
    val signUpSuccess: LiveData<Boolean> get() = _signUpSuccess

    /**
     * Sign up new user
     */
    fun signUp(email: String) {
        viewModelScope.launch {
            _isLoading.value = true

            signUpUseCase(email).handle(
                onSuccess = {
                    _signUpSuccess.value = true
                    _isLoading.value = false
                },
                onError = {
                    _signUpSuccess.value = false
                    _isLoading.value = false
                }
            )
        }
    }
}
