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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadImages에서 빈 리스트를 받으면 빈 상태를 유지한다`() = runTest {
        val viewModel = GalleryViewModel(
            albumRepository = FakeAlbumRepository(
                allImagesResult = Result.success(emptyList())
            )
        )

        viewModel.loadImages()
        advanceUntilIdle()

        assertEquals(0, viewModel.imgInfos.value?.size)
        assertEquals(0, viewModel.albumImgs.value?.size)
    }

    @Test
    fun `초기 상태에서는 선택 모드가 아니고 제거 목록이 비어있다`() {
        val viewModel = GalleryViewModel(
            albumRepository = FakeAlbumRepository(
                allImagesResult = Result.success(emptyList())
            )
        )

        assertFalse(viewModel.isSelectMode())
        assertTrue(viewModel.isRemoveListEmpty())
    }

    @Test
    fun `clearImages 후 invalid index 제거는 false를 반환한다`() {
        val viewModel = GalleryViewModel(
            albumRepository = FakeAlbumRepository(
                allImagesResult = Result.success(emptyList())
            )
        )

        viewModel.clearImages()
        assertEquals(0, viewModel.getImages().size)
        assertFalse(viewModel.removeImageAt(0))
        assertNotNull(viewModel.imgInfos.value)
    }

    private class FakeAlbumRepository(
        private val allImagesResult: Result<List<GalleryImageData>>
    ) : AlbumRepository {
        override suspend fun getAllImages(): Result<List<GalleryImageData>> = allImagesResult

        override suspend fun getImagesByDate(date: String): Result<List<AlbumImageData>> = Result.success(emptyList())

        override suspend fun getImageCount(): Result<Int> = Result.success(0)
    }
}
