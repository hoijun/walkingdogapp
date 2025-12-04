package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.core.location.LocationProvider
import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.usecase.walk.SaveWalkRecordUseCase
import com.tulmunchi.walkingdogapp.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val saveWalkRecordUseCase: SaveWalkRecordUseCase,
    private val locationProvider: LocationProvider
) : BaseViewModel() {

    private val _currentCoord = MutableLiveData<LatLng>()
    val currentCoord: LiveData<LatLng> get() = _currentCoord

    private val _walkSaved = MutableLiveData<Boolean>()
    val walkSaved: LiveData<Boolean> get() = _walkSaved

    /**
     * Save walk record
     */
    fun saveWalkRecord(
        dogNames: List<String>,
        startTime: String,
        distance: Float,
        time: Int,
        coords: List<LatLng>,
        collections: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // Convert Naver LatLng to Domain Coordinate
            val coordinates = coords.map {
                Coordinate(latitude = it.latitude, longitude = it.longitude)
            }

            saveWalkRecordUseCase(
                dogNames = dogNames,
                startTime = startTime,
                distance = distance,
                time = time,
                coords = coordinates,
                collections = collections
            ).handle(
                onSuccess = {
                    _walkSaved.value = true
                    _isLoading.value = false
                },
                onError = {
                    _walkSaved.value = false
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Get current location
     */
    fun getLastLocation() {
        viewModelScope.launch {
            locationProvider.getLastLocation()?.let { latLng ->
                _currentCoord.value = latLng
            }
        }
    }
}
