package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.data.repository.AlbumRepositoryImpl
import com.tulmunchi.walkingdogapp.domain.model.AlbumImageData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumMapViewModel @Inject constructor(
    private val albumRepository: AlbumRepositoryImpl
) : BaseViewModel() {

    /**
     * 선택된 날짜
     */
    private val _selectedDay = MutableLiveData<String>("")
    val selectedDay: LiveData<String> get() = _selectedDay

    /**
     * 앨범 이미지 데이터 (GPS 좌표 포함)
     */
    private val _albumImages = MutableLiveData<List<AlbumImageData>>(emptyList())
    val albumImages: LiveData<List<AlbumImageData>> get() = _albumImages

    /**
     * 이미지 존재 여부
     */
    private val _hasImages = MutableLiveData<Boolean>(false)
    val hasImages: LiveData<Boolean> get() = _hasImages

    /**
     * 스토리지 권한 허용 여부
     */
    private val _storagePermissionGranted = MutableLiveData<Boolean>(false)
    val storagePermissionGranted: LiveData<Boolean> get() = _storagePermissionGranted

    /**
     * 날짜를 선택하고 해당 날짜의 이미지를 로드합니다.
     * @param date 선택한 날짜 (yyyy-MM-dd 형식)
     */
    fun selectDate(date: String) {
        _selectedDay.value = date
        loadAlbumImages(date)
    }

    /**
     * 특정 날짜의 앨범 이미지를 로드합니다.
     * @param date 조회할 날짜 (yyyy-MM-dd 형식)
     */
    fun loadAlbumImages(date: String) {
        if (date.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            albumRepository.getImagesByDate(date).handle(
                onSuccess = { images ->
                    _albumImages.value = images
                    _hasImages.value = images.isNotEmpty()
                    _isLoading.value = false
                },
                onError = {
                    _hasImages.value = false
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * 스토리지 권한 허용 상태를 업데이트합니다.
     * @param granted 권한 허용 여부
     */
    fun setStoragePermissionGranted(granted: Boolean) {
        _storagePermissionGranted.value = granted
    }

    /**
     * 이미지 데이터를 초기화합니다.
     */
    fun clearImages() {
        _albumImages.value = emptyList()
        _hasImages.value = false
    }
}
