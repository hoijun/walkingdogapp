package com.tulmunchi.walkingdogapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.usecase.dog.AddDogUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.DeleteDogUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.UpdateDogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterDogViewModel @Inject constructor(
    private val addDogUseCase: AddDogUseCase,
    private val updateDogUseCase: UpdateDogUseCase,
    private val deleteDogUseCase: DeleteDogUseCase
) : BaseViewModel() {
    private val _dogUpdated = MutableLiveData<Boolean>()
    val dogUpdated: LiveData<Boolean> get() = _dogUpdated

    private val _dogDeleted = MutableLiveData<Boolean>()
    val dogDeleted: LiveData<Boolean> get() = _dogDeleted

    fun addDog(
        dog: Dog,
        imageUriString: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            addDogUseCase(dog, imageUriString).handle(
                onSuccess = {
                    _isLoading.value = false
                    _dogUpdated.value = true
                },
                onError = {
                    _isLoading.value = false
                    _dogUpdated.value = false
                }
            )
        }
    }

    /**
     * Update existing dog
     */
    fun updateDog(
        oldName: String,
        dog: Dog,
        imageUriString: String?,
        walkRecords: List<WalkRecord>,
        existingDogNames: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            updateDogUseCase(oldName, dog, imageUriString, walkRecords, existingDogNames).handle(
                onSuccess = {
                    _isLoading.value = false
                    _dogUpdated.value = true
                },
                onError = {
                    _isLoading.value = false
                    _dogUpdated.value = false
                }
            )
        }
    }

    /**
     * Delete dog
     */
    fun deleteDog(dogName: String) {
        viewModelScope.launch {
            _isLoading.value = true

            deleteDogUseCase(dogName).handle(
                onSuccess = {
                    _isLoading.value = false
                    _dogDeleted.value = true
                },
                onError = {
                    _isLoading.value = false
                    _dogDeleted.value = false
                }
            )
        }
    }
}
