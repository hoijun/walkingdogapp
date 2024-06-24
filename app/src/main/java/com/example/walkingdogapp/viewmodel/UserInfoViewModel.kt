package com.example.walkingdogapp.viewmodel

import android.Manifest
import android.app.Application
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
import com.example.walkingdogapp.album.GalleryImgInfo
import com.example.walkingdogapp.datamodel.AlarmDataModel
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.repository.UserInfoRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale


class UserInfoViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository = UserInfoRepository(application)
    private val _dogsInfo = MutableLiveData<List<DogInfo>>()
    private val _userInfo = MutableLiveData<UserInfo>()
    private val _totalWalkInfo = MutableLiveData<WalkInfo>()
    private val _walkDates = MutableLiveData<HashMap<String, MutableList<WalkRecord>>>()
    private val _collectionInfo = MutableLiveData<HashMap<String, Boolean>>()
    private val _dogsImg = MutableLiveData<HashMap<String, Uri>>()
    private val _successGetData = MutableLiveData<Boolean>()

    private val _currentCoord = MutableLiveData<LatLng>()
    private val _currentRegion = MutableLiveData<String>()
    private val _albumImgs = MutableLiveData<List<GalleryImgInfo>>()

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private lateinit var address : List<String>

    init {
        CoroutineScope(Dispatchers.IO).launch {
            repository.observeUser(
                _dogsInfo,
                _userInfo,
                _totalWalkInfo,
                _walkDates,
                _collectionInfo,
                _dogsImg,
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

    val totalWalkInfo: LiveData<WalkInfo>
        get() = _totalWalkInfo

    val walkDates: LiveData<HashMap<String, MutableList<WalkRecord>>>
        get() = _walkDates

    val collectionInfo: LiveData<HashMap<String, Boolean>>
        get() = _collectionInfo

    val albumImgs: LiveData<List<GalleryImgInfo>>
        get() = _albumImgs

    val dogsImg: LiveData<HashMap<String, Uri>>
        get() = _dogsImg

    val currentRegion: LiveData<String>
        get() = _currentRegion

    val successGetData: LiveData<Boolean>
        get() = _successGetData

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

    suspend fun updateUserInfo(userInfo: UserInfo) {
       repository.updateUserInfo(userInfo)
    }

    suspend fun updateDogInfo(dogInfo: DogInfo, beforeName: String, imgUri: Uri?, walkRecords: ArrayList<WalkRecord>): Boolean {
        return repository.updateDogInfo(dogInfo, beforeName, imgUri, walkRecords)
    }

    suspend fun removeDogInfo(beforeName: String) {
        repository.removeDogInfo(beforeName)
    }

    suspend fun saveWalkInfo(walkDogs: ArrayList<String>, startTime: String, distance: Float, time: Int, coords: List<com.naver.maps.geometry.LatLng>, collections: List<String>): Boolean {
        return repository.saveWalkInfo(walkDogs, startTime, distance, time, coords, collections)
    }

    suspend fun removeAccount() : Boolean{
        return repository.removeAccount()
    }

    private fun updateLocateInfo(input: LatLng) {
        _currentCoord.value = input
    }


    // 현재 좌표
    fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION)
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
        val geocoder = Geocoder(application, Locale.getDefault())
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