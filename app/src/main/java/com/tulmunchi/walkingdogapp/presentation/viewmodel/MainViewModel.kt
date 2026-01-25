package com.tulmunchi.walkingdogapp.presentation.viewmodel

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.core.location.LocationProvider
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.model.WeatherResponse
import com.tulmunchi.walkingdogapp.domain.usecase.LoadInitialDataUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.weather.GetWeatherForecastUseCase
import com.tulmunchi.walkingdogapp.presentation.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val loadInitialDataUseCase: LoadInitialDataUseCase,
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase,
    private val locationProvider: LocationProvider
) : BaseViewModel() {

    // User data
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    // Dogs data
    private val _dogs = MutableLiveData<List<Dog>>()
    val dogs: LiveData<List<Dog>> get() = _dogs

    private val _dogNames = MutableLiveData<List<String>>()
    val dogNames: LiveData<List<String>> get() = _dogNames

    private val _dogImages = MutableLiveData<Map<String, String>>()
    val dogImages: LiveData<Map<String, String>> get() = _dogImages

    // Walk data
    private val _totalWalkStats = MutableLiveData<WalkStats>()
    val totalWalkStats: LiveData<WalkStats> get() = _totalWalkStats

    private val _walkHistory = MutableLiveData<Map<String, List<WalkRecord>>>()
    val walkHistory: LiveData<Map<String, List<WalkRecord>>> get() = _walkHistory

    // Collections
    private val _collections = MutableLiveData<Map<String, Boolean>>()
    val collections: LiveData<Map<String, Boolean>> get() = _collections

    // Location
    private val _currentCoord = MutableLiveData<LatLng>()
    val currentCoord: LiveData<LatLng> get() = _currentCoord

    private val _currentRegion = MutableLiveData<String>()
    val currentRegion: LiveData<String> get() = _currentRegion

    // Success flags
    private val _dataLoadSuccess = MutableLiveData<Boolean>()
    val dataLoadSuccess: LiveData<Boolean> get() = _dataLoadSuccess

    private val _weatherInfo = MutableLiveData<WeatherInfo>()
    val weatherInfo: LiveData<WeatherInfo> get() = _weatherInfo

    /**
     * Load all initial user data
     */
    fun loadUserData(loadImages: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true

            loadInitialDataUseCase(loadImages).handle(
                onSuccess = { initialData ->
                    _user.value = initialData.user
                    _dogs.value = initialData.dogs
                    _dogNames.value = initialData.dogs.map { it.name }
                    if (loadImages) _dogImages.value = initialData.dogImages
                    _totalWalkStats.value = initialData.totalWalkStats
                    _walkHistory.value = initialData.walkHistory
                    _collections.value = initialData.collections
                    _dataLoadSuccess.value = true
                    _isLoading.value = false
                },
                onError = {
                    _dataLoadSuccess.value = false
                    _isLoading.value = false
                }
            )
        }
    }

    fun updateUser(updatedUser: User) {
        viewModelScope.launch {
            _user.value = updatedUser
        }
    }

    fun updateDog(updatedDog: Dog, beforeName: String, uri: String?) {
        viewModelScope.launch {
            if (beforeName == "") {
                _dogs.value = _dogs.value?.toMutableList()?.apply { add(updatedDog) }
                _dogNames.value = _dogNames.value?.toMutableList()?.apply { add(updatedDog.name) }
                uri?.let { _dogImages.value = _dogImages.value?.toMutableMap()?.apply { this[updatedDog.name] = it } }
                return@launch
            }

            if (beforeName == updatedDog.name && uri == null) {
                _dogs.value = _dogs.value?.map { if (it.name == beforeName) updatedDog else it }
            }

            if (beforeName != updatedDog.name && uri == null) {
                _dogNames.value = _dogNames.value?.map { if (it == beforeName) updatedDog.name else it }
                _dogs.value = _dogs.value?.map { if (it.name == beforeName) updatedDog else it }
                _dogImages.value = _dogImages.value?.toMutableMap()?.apply {
                    this[updatedDog.name] = this[beforeName] ?: ""
                    this.remove(beforeName)
                }
            }

            if (beforeName == updatedDog.name && uri != null) {
                _dogImages.value = _dogImages.value?.toMutableMap()?.apply { this[beforeName] = uri }
            }

            if (beforeName != updatedDog.name && uri != null) {
                _dogNames.value = _dogNames.value?.map { if (it == beforeName) updatedDog.name else it }
                _dogs.value = _dogs.value?.map { if (it.name == beforeName) updatedDog else it }
                _dogImages.value = _dogImages.value?.toMutableMap()?.apply {
                    this[updatedDog.name] = uri
                    this.remove(beforeName)
                }
            }
        }
    }

    fun deleteDog(name: String) {
        viewModelScope.launch {
            _dogNames.value = _dogNames.value?.filter { it != name }
            _dogs.value = _dogs.value?.filter { it.name != name }
            _dogImages.value = _dogImages.value?.toMutableMap()?.apply { this.remove(name) }
        }
    }

    /**
     * Get current location
     */
    fun getLastLocation() {
        viewModelScope.launch {
            locationProvider.getLastLocation()?.let { latLng ->
                _currentCoord.value = latLng
                getCurrentAddress(latLng)
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

    /**
     * Check if data has been successfully loaded
     */
    fun isSuccessGetData(): Boolean {
        return _dataLoadSuccess.value == true
    }

    /**
     * Convert coordinates to address
     */
    private fun getCurrentAddress(coord: LatLng) {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                val addresses: MutableList<Address> = geocoder.getFromLocation(
                    coord.latitude,
                    coord.longitude, 7
                ) ?: mutableListOf()

                if (addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0).split(" ").takeLast(3)
                    val nameofLoc = address.getOrNull(0)?.let { first ->
                        address.getOrNull(1)?.let { second ->
                            "$first $second"
                        }
                    } ?: ""
                    _currentRegion.postValue(nameofLoc)
                }
            } catch (e: IOException) {
                _currentRegion.postValue("")
            }
        } else {
            geocoder.getFromLocation(
                coord.latitude, coord.longitude, 7,
                @RequiresApi(33) object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0].getAddressLine(0).split(" ").takeLast(3)
                            val nameofLoc = address.getOrNull(0)?.let { first ->
                                address.getOrNull(1)?.let { second ->
                                    "$first $second"
                                }
                            } ?: ""
                            _currentRegion.postValue(nameofLoc)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        super.onError(errorMessage)
                        _currentRegion.postValue("")
                    }
                }
            )
        }
    }
}
