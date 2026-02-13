package com.tulmunchi.walkingdogapp.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.naver.maps.geometry.LatLng
import com.tulmunchi.walkingdogapp.domain.model.AlbumImageData
import com.tulmunchi.walkingdogapp.domain.model.GalleryImageData
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * 모든 갤러리 이미지를 조회합니다.
     * @return 모든 이미지 정보 리스트
     */
    suspend fun getAllImages(): Result<List<GalleryImageData>> = withContext(Dispatchers.IO) {
        try {
            val images = mutableListOf<GalleryImageData>()
            val contentResolver = context.contentResolver
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.ORIENTATION
            )

            val cursor = contentResolver.query(
                uri,
                projection,
                getCommonSelection(),
                getCommonSelectionArgs(),
                "${MediaStore.Images.Media.DATE_TAKEN} ASC"
            )

            cursor?.use {
                val columnIndexId = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val columnIndexDate = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val columnIndexWidth = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val columnIndexHeight = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val columnIndexOrientation = it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

                while (it.moveToNext()) {
                    val imageId = it.getString(columnIndexId)
                    val imageDate = it.getLong(columnIndexDate)
                    val imageWidth = it.getInt(columnIndexWidth)
                    val imageHeight = it.getInt(columnIndexHeight)
                    val orientation = it.getInt(columnIndexOrientation)
                    val contentUri = Uri.withAppendedPath(uri, imageId)

                    // orientation에 따라 width/height 조정
                    val (finalWidth, finalHeight) = when (orientation) {
                        90, 270 -> Pair(imageHeight, imageWidth)
                        else -> Pair(imageWidth, imageHeight)
                    }

                    images.add(
                        GalleryImageData(
                            uriString = contentUri.toString(),
                            dateTaken = imageDate,
                            width = finalWidth,
                            height = finalHeight
                        )
                    )
                }
            }

            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 특정 날짜의 GPS 좌표가 있는 이미지를 조회합니다.
     * @param date 조회할 날짜 (yyyy-MM-dd 형식)
     * @return GPS 좌표가 포함된 이미지 리스트 (최대 20개)
     */
    suspend fun getImagesByDate(date: String): Result<List<AlbumImageData>> = withContext(Dispatchers.IO) {
        try {
            val images = mutableListOf<AlbumImageData>()
            val contentResolver = context.contentResolver
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN
            )

            val cursor = contentResolver.query(
                uri,
                projection,
                getCommonSelection(),
                getCommonSelectionArgs(),
                "${MediaStore.Images.Media.DATE_TAKEN} ASC"
            )

            cursor?.use {
                val columnIndexId = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val columnIndexDate = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (it.moveToNext()) {
                    val imageDate = DateUtils.convertLongToTime(
                        SimpleDateFormat("yyyy-MM-dd"),
                        it.getLong(columnIndexDate) / 1000L
                    )

                    if (imageDate == date) {
                        val imageId = it.getString(columnIndexId)
                        val contentUri = Uri.withAppendedPath(uri, imageId)

                        // EXIF GPS 좌표 추출
                        getLatLngFromExif(contentUri)?.let { latLng ->
                            images.add(
                                AlbumImageData(
                                    uriString = contentUri.toString(),
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude
                                )
                            )
                        }

                        if (images.size == 20) break
                    }
                }
            }

            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 앨범 이미지 개수를 조회합니다.
     * @return 이미지 개수
     */
    suspend fun getImageCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            val contentResolver = context.contentResolver
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media._ID)

            val cursor = contentResolver.query(
                uri,
                projection,
                getCommonSelection(),
                getCommonSelectionArgs(),
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    count++
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 공통 쿼리 조건 생성
     */
    private fun getCommonSelection(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.IS_PENDING} = 0"
        } else {
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        }
    }

    /**
     * 공통 쿼리 인자
     */
    private fun getCommonSelectionArgs(): Array<String> {
        return arrayOf("털뭉치", "%munchi_%")
    }

    /**
     * EXIF 데이터에서 GPS 좌표를 추출합니다.
     * @param uri 이미지 URI
     * @return GPS 좌표 (없으면 null)
     */
    private fun getLatLngFromExif(uri: Uri): LatLng? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLng = exifInterface?.getLatLong()

            if (latLng != null) {
                LatLng(latLng[0], latLng[1])
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
