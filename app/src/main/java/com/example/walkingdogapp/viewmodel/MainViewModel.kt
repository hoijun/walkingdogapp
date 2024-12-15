package com.example.walkingdogapp.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.walkingdogapp.datamodel.GalleryImgInfo
import com.example.walkingdogapp.datamodel.AlarmDataModel
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.TotalWalkInfo
import com.example.walkingdogapp.datamodel.WalkDateInfo
import com.example.walkingdogapp.repository.UserInfoRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UserInfoRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {
    private val _dogsInfo = MutableLiveData<List<DogInfo>>()
    private val _userInfo = MutableLiveData<UserInfo>()
    private val _totalTotalWalkInfo = MutableLiveData<TotalWalkInfo>()
    private val _walkDates = MutableLiveData<HashMap<String, MutableList<WalkDateInfo>>>()
    private val _collectionWhether = MutableLiveData<HashMap<String, Boolean>>()
    private val _dogsImg = MutableLiveData<HashMap<String, Uri>>()
    private val _dogNames = MutableLiveData<List<String>>()
    private val _successGetData = MutableLiveData<Boolean>()

    private val _currentCoord = MutableLiveData<LatLng>()
    private val _currentRegion = MutableLiveData<String>()
    private val _albumImgs = MutableLiveData<List<GalleryImgInfo>>()

    private lateinit var address : List<String>

    init {
        CoroutineScope(Dispatchers.IO).launch {
            repository.observeUser(
                _dogsInfo,
                _userInfo,
                _totalTotalWalkInfo,
                _walkDates,
                _collectionWhether,
                _dogsImg,
                _dogNames,
                _successGetData
            )
        }
    }

    val currentCoord: LiveData<LatLng>
        get() = _currentCoord

    val dogsInfo: LiveData<List<DogInfo>>
        get() = _dogsInfo

    val userInfo: LiveData<UserInfo>
        get() = _userInfo

    val totalWalkInfo: LiveData<TotalWalkInfo>
        get() = _totalTotalWalkInfo

    val walkDates: LiveData<HashMap<String, MutableList<WalkDateInfo>>>
        get() = _walkDates

    val collectionWhether: LiveData<HashMap<String, Boolean>>
        get() = _collectionWhether

    val albumImgs: LiveData<List<GalleryImgInfo>>
        get() = _albumImgs

    val dogsImg: LiveData<HashMap<String, Uri>>
        get() = _dogsImg

    val dogsNames: LiveData<List<String>>
        get() = _dogNames

    val currentRegion: LiveData<String>
        get() = _currentRegion

    val successGetData: LiveData<Boolean>
        get() = _successGetData

    fun isSuccessGetData(): Boolean = successGetData.value!!

    fun saveAlbumImgs(albumImgs: List<GalleryImgInfo>) {
        _albumImgs.value = albumImgs
    }

    fun deleteAlarm(alarm: AlarmDataModel) {
        repository.delete(alarm)
    }

    fun addAlarm(alarm: AlarmDataModel) {
        repository.add(alarm)
    }

    fun getAlarmList(): List<AlarmDataModel> {
        return repository.getAll()
    }

    fun onOffAlarm(alarm: AlarmDataModel, isChecked: Boolean) {
        repository.onOffAlarm(alarm.alarm_code, isChecked)
    }

    fun observeUser() {
        CoroutineScope(Dispatchers.IO).launch {
            repository.observeUser(
                _dogsInfo,
                _userInfo,
                _totalTotalWalkInfo,
                _walkDates,
                _collectionWhether,
                _dogsImg,
                _dogNames,
                _successGetData
            )
        }
        getLastLocation()
    }

    suspend fun removeAccount() : Boolean {
        return repository.removeAccount()
    }

    private fun updateLocateInfo(input: LatLng) {
        _currentCoord.value = input
    }


    // 현재 좌표
    fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    updateLocateInfo(LatLng(it.latitude, it.longitude))
                    getCurrentAddress(LatLng(it.latitude, it.longitude))
                }
            }
        }
    }

    // 현재 좌표를 주소로 변경
    private fun getCurrentAddress(coord: LatLng) {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                val addresses: MutableList<Address> = geocoder.getFromLocation(
                    coord.latitude,
                    coord.longitude, 7)!!
                address = addresses[0].getAddressLine(0).split(" ").takeLast(3)
                val nameofLoc = address[0] + " " + address[1]
                _currentRegion.postValue(nameofLoc)
            } catch(e: IOException) {
                _currentRegion.postValue("")
            }
        } else {
            geocoder.getFromLocation(coord.latitude, coord.longitude,
                7,
                @RequiresApi(33) object :
                Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    address = addresses[0].getAddressLine(0).split(" ").takeLast(3)
                    val nameofLoc = address[0] + " " + address[1]
                    Log.d("locate", "get locate")
                    _currentRegion.postValue(nameofLoc)
                }
                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                    _currentRegion.postValue("")
                }
            })
        }
    }
}