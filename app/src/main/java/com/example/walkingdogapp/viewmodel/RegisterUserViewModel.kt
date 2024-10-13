package com.example.walkingdogapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterUserViewModel @Inject constructor(private val repository: UserInfoRepository): ViewModel() {
    suspend fun updateUserInfo(userInfo: UserInfo) {
        repository.updateUserInfo(userInfo)
    }
}