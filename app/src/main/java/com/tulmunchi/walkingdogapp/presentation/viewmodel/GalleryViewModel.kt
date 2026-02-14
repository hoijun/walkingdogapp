package com.tulmunchi.walkingdogapp.presentation.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tulmunchi.walkingdogapp.domain.repository.AlbumRepository
import com.tulmunchi.walkingdogapp.presentation.model.GalleryImgInfo
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {

    private val _imgInfos = MutableLiveData<MutableList<GalleryImgInfo>>(mutableListOf())
    val imgInfos: LiveData<MutableList<GalleryImgInfo>> get() = _imgInfos

    private val _removeImgList = MutableLiveData<MutableSet<Uri>>(mutableSetOf())

    private val _selectMode = MutableLiveData(false)
    val selectMode: LiveData<Boolean> get() = _selectMode

    // Album images (shared between Gallery and DetailPicture)
    private val _albumImgs = MutableLiveData<List<GalleryImgInfo>>(emptyList())
    val albumImgs: LiveData<List<GalleryImgInfo>> get() = _albumImgs

    /**
     * Load all gallery images from repository
     */
    fun loadImages() {
        viewModelScope.launch {
            clearImages()

            albumRepository.getAllImages().handle(
                onSuccess = { images ->
                    val galleryImages = images.map { data ->
                        GalleryImgInfo(
                            uri = data.uriString.toUri(),
                            date = DateUtils.convertLongToTime(
                                SimpleDateFormat("yyyy년 MM월 dd일 HH:mm"),
                                data.dateTaken / 1000L
                            ),
                            width = data.width,
                            height = data.height
                        )
                    }.toMutableList()

                    _imgInfos.value = galleryImages
                    saveAlbumImgs(galleryImages)
                },
                onError = { throwable ->
                    _error.postValue(throwable.message)
                }
            )
        }
    }

    /**
     * Clear all images
     */
    fun clearImages() {
        _imgInfos.value = mutableListOf()
    }

    /**
     * Get current image list
     */
    fun getImages(): List<GalleryImgInfo> {
        return _imgInfos.value ?: emptyList()
    }

    /**
     * Enter select mode
     */
    fun enterSelectMode() {
        _selectMode.value = true
    }

    /**
     * Exit select mode and clear selection
     */
    fun exitSelectMode() {
        _selectMode.value = false
        _removeImgList.value = mutableSetOf()
    }

    /**
     * Toggle image selection for deletion
     */
    fun toggleImageSelection(uri: Uri) {
        val set = _removeImgList.value ?: mutableSetOf()
        if (set.contains(uri)) {
            set.remove(uri)
        } else {
            set.add(uri)
        }
        _removeImgList.value = set
    }

    /**
     * Get list of URIs to remove
     */
    fun getRemoveList(): List<Uri> {
        return _removeImgList.value?.toList() ?: emptyList()
    }

    /**
     * Check if remove list is empty
     */
    fun isRemoveListEmpty(): Boolean {
        return _removeImgList.value?.isEmpty() ?: true
    }

    /**
     * Check if currently in select mode
     */
    fun isSelectMode(): Boolean {
        return _selectMode.value == true
    }

    /**
     * Remove image at specific index
     */
    fun removeImageAt(index: Int): Boolean {
        val currentList = _imgInfos.value ?: return false
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _imgInfos.value = currentList
            return true
        }
        return false
    }

    /**
     * Save album images for sharing between Gallery and DetailPicture
     */
    fun saveAlbumImgs(images: List<GalleryImgInfo>) {
        _albumImgs.value = images
    }
}
