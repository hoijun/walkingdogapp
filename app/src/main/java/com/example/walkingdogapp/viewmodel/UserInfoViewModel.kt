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
import com.example.walkingdogapp.datamodel.WalkDate
import com.example.walkingdogapp.repository.UserInfoRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale


class UserInfoViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _dogsinfo = MutableLiveData<List<DogInfo>>()
    private val _userinfo = MutableLiveData<UserInfo>()
    private val _totalwalkinfo = MutableLiveData<WalkInfo>()
    private val _walkDates = MutableLiveData<List<WalkDate>>()
    private val _collectioninfo = MutableLiveData<HashMap<String, Boolean>>()
    private val _dogsimg = MutableLiveData<HashMap<String, Uri>>()

    private val _currentCoord = MutableLiveData<LatLng>()
    private val _albumImgs = MutableLiveData<List<GalleryImgInfo>>()
    private val repository = UserInfoRepository(application)

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private lateinit var address : List<String>

    val currentCoord: LiveData<LatLng>
        get() = _currentCoord

    val dogsinfo: LiveData<List<DogInfo>>
        get() = _dogsinfo

    val userinfo: LiveData<UserInfo>
        get() = _userinfo

    val totalwalkinfo: LiveData<WalkInfo>
        get() = _totalwalkinfo

    val walkDates: LiveData<List<WalkDate>>
        get() = _walkDates

    val collectioninfo: LiveData<HashMap<String, Boolean>>
        get() = _collectioninfo

    val albumImgs: LiveData<List<GalleryImgInfo>>
        get() = _albumImgs

    val dogsimg: LiveData<HashMap<String, Uri>>
        get() = _dogsimg

    private fun upadateLocateInfo(input: LatLng) {
        _currentCoord.value = input
    }

    fun savedogsInfo(dogsinfo: List<DogInfo>) {
        _dogsinfo.value = dogsinfo
    }

    fun saveuserInfo(userInfo: UserInfo) {
        _userinfo.value = userInfo
    }

    fun savetotalwalkInfo(totalwalkInfo: WalkInfo) {
        _totalwalkinfo.value = totalwalkInfo
    }

    fun savewalkdates(walkDates: List<WalkDate>) {
        _walkDates.value = walkDates
    }

    fun savecollectionInfo(collectioninfo: HashMap<String, Boolean>){
        _collectioninfo.value = collectioninfo
    }

    fun savealbumImgs(albumImgs: List<GalleryImgInfo>) {
        _albumImgs.value = albumImgs
    }

    fun savedogsImg(dogsImg: HashMap<String, Uri>) {
        _dogsimg.value = dogsImg
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

    fun onOffAlarm(alarm: AlarmDataModel, ischecked: Boolean) {
        repository.onOffAlarm(alarm.alarm_code, ischecked)
    }


    // 현재 좌표
    fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    upadateLocateInfo(LatLng(it.latitude, it.longitude))
                }
            }
        }
    }

    // 현재 좌표를 주소로 변경
    fun getCurrentAddress(coord: LatLng, callback: (String) -> Unit) {
        val geocoder = Geocoder(application, Locale.getDefault())
        if (Build.VERSION.SDK_INT < 33) {
            try {
                val addresses: MutableList<Address> = geocoder.getFromLocation(
                    coord.latitude,
                    coord.longitude, 7)!!
                address = addresses[0].getAddressLine(0).split(" ").takeLast(3)
                val nameofLoc = address[0] + " " + address[1]
                callback(nameofLoc)
            } catch(e: IOException) {
                callback("지역")
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
                    callback(nameofLoc)
                }
                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                    callback("지역")
                }
            })
        }
    }
}