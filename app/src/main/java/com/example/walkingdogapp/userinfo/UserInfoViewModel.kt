package com.example.walkingdogapp.userinfo

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale


class userInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentCoord = MutableLiveData<LatLng>()
    private val _doginfo = MutableLiveData<DogInfo>()
    private val _userinfo = MutableLiveData<UserInfo>()
    private val _imguri = MutableLiveData<Uri>()
    private val _imgdrawable = MutableLiveData<Drawable>()
    private var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var address : List<String>
    private val applic = application

    val currentCoord: LiveData<LatLng>
        get() = _currentCoord

    val imguri: LiveData<Uri>
        get() = _imguri

    val imgdrawble: LiveData<Drawable>
        get() = _imgdrawable

    val doginfo: LiveData<DogInfo>
        get() = _doginfo

    val userinfo: LiveData<UserInfo>
        get() = _userinfo

    init {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applic)
    }

    private fun upadateLocateInfo(input: LatLng) {
        _currentCoord.value = input
    }

    fun saveImgUri(uri: Uri) {
        _imguri.value = uri
    }

    fun saveImgDrawble(drawable: Drawable) {
        _imgdrawable.value = drawable
    }

    fun savedogInfo(doginfo: DogInfo) {
        _doginfo.value = doginfo
    }

    fun saveuserInfo(userInfo: UserInfo) {
        _userinfo.value = userInfo
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
        val geocoder = Geocoder(applic, Locale.getDefault())
        if (Build.VERSION.SDK_INT < 33) {
            Log.d("SDK", "LESSTHAN33")
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
            Log.d("SDK", "MOERTHAN33")
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