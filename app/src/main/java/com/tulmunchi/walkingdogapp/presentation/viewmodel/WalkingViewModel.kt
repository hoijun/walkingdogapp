package com.tulmunchi.walkingdogapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.core.location.LocationProvider
import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.usecase.walk.SaveWalkRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        calories: Float,
        poopLatLngs: List<LatLng>,
        memoLatLngs: Map<LatLng, String>,
        walkLatLngs: List<LatLng>,
        collections: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // Convert Naver LatLng to Domain Coordinate
            val walkCoordinates = walkLatLngs.map {
                Coordinate(latitude = it.latitude, longitude = it.longitude)
            }

            val poopCoordinates = poopLatLngs.map {
                Coordinate(latitude = it.latitude, longitude = it.longitude)
            }

            val memoCoordinates = memoLatLngs.entries.associate { (latLng, memo) ->
                memo to Coordinate(latitude = latLng.latitude, longitude = latLng.longitude)
            }

            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val walkRecord = WalkRecord(
                day = day,
                startTime = startTime,
                endTime = currentTime,
                distance = distance,
                time = time,
                calories = calories,
                poopCoordinates = poopCoordinates,
                memoCoordinates = memoCoordinates,
                walkCoordinates = walkCoordinates,
                collections = collections
            )

            saveWalkRecordUseCase(
                dogNames = dogNames,
                walkRecord = walkRecord
            ).handle(
                onSuccess = {
                    _walkSaved.value = true
                    _isLoading.value = false
                },
                onError = {
                    Log.e("WalkingViewModel", "saveWalkRecord: ${it.message}")
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
