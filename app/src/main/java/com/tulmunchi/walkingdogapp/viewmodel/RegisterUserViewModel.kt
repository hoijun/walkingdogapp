package com.tulmunchi.walkingdogapp.viewmodel

import androidx.lifecycle.ViewModel
import com.tulmunchi.walkingdogapp.datamodel.UserInfo
import com.tulmunchi.walkingdogapp.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterUserViewModel @Inject constructor(private val repository: UserInfoRepository): ViewModel() {
    suspend fun updateUserInfo(userInfo: UserInfo) {
        repository.updateUserInfo(userInfo)
    }
}