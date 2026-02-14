package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.tulmunchi.walkingdogapp.domain.model.AlbumImageData
import com.tulmunchi.walkingdogapp.domain.model.GalleryImageData
import com.tulmunchi.walkingdogapp.domain.repository.AlbumRepository
import com.tulmunchi.walkingdogapp.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumMapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selectDate는 날짜를 저장하고 이미지를 로드한다`() = runTest {
        val images = listOf(
            AlbumImageData(
                uriString = "content://image/1",
                latitude = 37.5,
                longitude = 127.0
            )
        )
        val viewModel = AlbumMapViewModel(
            albumRepository = FakeAlbumRepository(
                imagesByDateResult = Result.success(images)
            )
        )

        viewModel.selectDate("2026-02-14")
        advanceUntilIdle()

        assertEquals("2026-02-14", viewModel.selectedDay.value)
        assertEquals(1, viewModel.albumImages.value?.size)
        assertTrue(viewModel.hasImages.value == true)
        assertFalse(viewModel.isLoading.value == true)
    }

    @Test
    fun `loadAlbumImages 실패 시 hasImages는 false로 설정된다`() = runTest {
        val viewModel = AlbumMapViewModel(
            albumRepository = FakeAlbumRepository(
                imagesByDateResult = Result.failure(IllegalStateException("album fail"))
            )
        )

        viewModel.loadAlbumImages("2026-02-14")
        advanceUntilIdle()

        assertTrue(viewModel.hasImages.value == false)
        assertFalse(viewModel.isLoading.value == true)
    }

    @Test
    fun `clearImages는 이미지와 hasImages를 초기화한다`() {
        val viewModel = AlbumMapViewModel(
            albumRepository = FakeAlbumRepository(imagesByDateResult = Result.success(emptyList()))
        )

        viewModel.clearImages()

        assertEquals(emptyList(), viewModel.albumImages.value)
        assertTrue(viewModel.hasImages.value == false)
    }

    private class FakeAlbumRepository(
        private val imagesByDateResult: Result<List<AlbumImageData>>
    ) : AlbumRepository {
        override suspend fun getAllImages(): Result<List<GalleryImageData>> = Result.success(emptyList())

        override suspend fun getImagesByDate(date: String): Result<List<AlbumImageData>> = imagesByDateResult

        override suspend fun getImageCount(): Result<Int> = Result.success(0)
    }
}
