package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : BaseViewModel() {

    /**
     * 앨범 이미지 개수
     */
    private val _imageCount = MutableLiveData<Int>(0)
    val imageCount: LiveData<Int> get() = _imageCount

    /**
     * 앨범 이미지 개수를 로드합니다.
     */
    fun loadImageCount() {
        viewModelScope.launch {
            albumRepository.getImageCount().handle(
                onSuccess = { count ->
                    _imageCount.value = count
                },
                onError = {
                    _imageCount.value = 0
                }
            )
        }
    }
}
