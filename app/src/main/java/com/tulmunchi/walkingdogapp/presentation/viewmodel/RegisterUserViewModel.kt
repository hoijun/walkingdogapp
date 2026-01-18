package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.usecase.user.UpdateUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterUserViewModel @Inject constructor(
    private val updateUserInfoUseCase: UpdateUserInfoUseCase
) : BaseViewModel() {

    private val _userUpdated = MutableLiveData<Boolean>()
    val userUpdated: LiveData<Boolean> get() = _userUpdated

    /**
     * Update user information
     */
    fun updateUserInfo(user: User) {
        viewModelScope.launch {
            _isLoading.value = true

            updateUserInfoUseCase(user).handle(
                onSuccess = {
                    _isLoading.value = false
                    _userUpdated.value = true
                },
                onError = {
                    _isLoading.value = false
                    _userUpdated.value = false
                }
            )
        }
    }
}
