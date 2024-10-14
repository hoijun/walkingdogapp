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
    suspend fun updateDogInfo(
        dogInfo: DogInfo,
        beforeName: String,
        imgUri: Uri?,
        walkDateInfos: ArrayList<WalkDateInfo>,
        dogUriList: HashMap<String, Uri>,
        dogNameList: List<String>
    ): Boolean {
        return repository.updateDogInfo(dogInfo, beforeName, imgUri, walkDateInfos, dogUriList, dogNameList)
    }

    suspend fun removeDogInfo(beforeName: String, dogUriList: HashMap<String, Uri>) {
        repository.removeDogInfo(beforeName, dogUriList)
    }
}