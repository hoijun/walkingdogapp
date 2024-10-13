package com.example.walkingdogapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.WalkDateInfo
import com.example.walkingdogapp.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterDogViewModel @Inject constructor(private val repository: UserInfoRepository) : ViewModel() {
    suspend fun updateDogInfo(dogInfo: DogInfo, beforeName: String, imgUri: Uri?, walkDateInfos: ArrayList<WalkDateInfo>): Boolean {
        return repository.updateDogInfo(dogInfo, beforeName, imgUri, walkDateInfos)
    }

    suspend fun removeDogInfo(beforeName: String) {
        repository.removeDogInfo(beforeName)
    }
}