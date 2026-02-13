package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.usecase.user.SignUpUseCase
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
                    _isLoading.value = false
                    _signUpSuccess.value = true
                },
                onError = {
                    _isLoading.value = false
                    _signUpSuccess.value = false
                }
            )
        }
    }
}
