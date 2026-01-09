package com.tulmunchi.walkingdogapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.core.location.LocationProvider
import com.tulmunchi.walkingdogapp.domain.model.Coordinate
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import com.tulmunchi.walkingdogapp.domain.usecase.walk.SaveWalkRecordUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.weather.GetWeatherForecastUseCase
import com.tulmunchi.walkingdogapp.presentation.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val saveWalkRecordUseCase: SaveWalkRecordUseCase,
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase,
    private val locationProvider: LocationProvider
) : BaseViewModel() {

    private val _currentCoord = MutableLiveData<LatLng>()
    val currentCoord: LiveData<LatLng> get() = _currentCoord

    private val _walkSaved = MutableLiveData<Boolean>()
    val walkSaved: LiveData<Boolean> get() = _walkSaved

    private val _weatherInfo = MutableLiveData<WeatherInfo>()
    val weatherInfo: LiveData<WeatherInfo> get() = _weatherInfo



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
                getWeatherForecast(latLng)
            }
        }
    }

    suspend fun getWeatherForecast(latLng: LatLng) {
        val cal = Calendar.getInstance()
        val cur = Locale.getDefault()

        cal.add(Calendar.MINUTE, -45)

        val baseDate = SimpleDateFormat("yyyyMMdd", cur).format(cal.time)
        val hour = SimpleDateFormat("HH00", cur).format(cal.time)
        getWeatherForecastUseCase(
            baseDate = baseDate,
            baseTime = hour,
            lat = latLng.latitude,
            lon = latLng.longitude
        ).handle(
            onSuccess = {
                _weatherInfo.postValue(getWeatherIcon(it))
            },
            onError = {
                _error.postValue(it.message)
            }
        )
    }

    private fun getWeatherIcon(weatherResponse: WeatherResponse): WeatherInfo {
        val sky = weatherResponse.sky.toInt()
        val pty = weatherResponse.pty.toInt()

        return when (pty) {
            0 -> when (sky) {
                1 -> WeatherInfo("맑음", "☀️")
                2 -> WeatherInfo("구름조금", "🌤️")
                3 -> WeatherInfo("구름많음", "⛅")
                4 -> WeatherInfo("흐림", "☁️")
                else -> WeatherInfo("맑음", "☀️")
            }
            1 -> WeatherInfo("비", "🌧️")
            2 -> WeatherInfo("비/눈", "🌨️")
            3 -> WeatherInfo("눈/비", "🌨️")
            4 -> WeatherInfo("눈", "❄️")
            else -> WeatherInfo("맑음", "☀️")
        }
    }
}
