package com.tulmunchi.walkingdogapp.domain.repository

import com.tulmunchi.walkingdogapp.domain.model.AlbumImageData
import com.tulmunchi.walkingdogapp.domain.model.GalleryImageData

/**
 * Repository interface for album/gallery-related operations
 */
interface AlbumRepository {
    /**
     * Get all gallery images from device storage
     */
    suspend fun getAllImages(): Result<List<GalleryImageData>>

    /**
     * Get images with GPS info for a specific date
     */
    suspend fun getImagesByDate(date: String): Result<List<AlbumImageData>>

    /**
     * Get image count from device storage
     */
    suspend fun getImageCount(): Result<Int>
}
