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

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `이미지 개수 로드 성공 시 count를 반영한다`() = runTest {
        val viewModel = MyPageViewModel(
            albumRepository = FakeAlbumRepository(
                imageCountResult = Result.success(12)
            )
        )

        viewModel.loadImageCount()
        advanceUntilIdle()

        assertEquals(12, viewModel.imageCount.value)
    }

    @Test
    fun `이미지 개수 로드 실패 시 count를 0으로 설정한다`() = runTest {
        val viewModel = MyPageViewModel(
            albumRepository = FakeAlbumRepository(
                imageCountResult = Result.failure(IllegalStateException("count fail"))
            )
        )

        viewModel.loadImageCount()
        advanceUntilIdle()

        assertEquals(0, viewModel.imageCount.value)
    }

    private class FakeAlbumRepository(
        private val imageCountResult: Result<Int>
    ) : AlbumRepository {
        override suspend fun getAllImages(): Result<List<GalleryImageData>> = Result.success(emptyList())

        override suspend fun getImagesByDate(date: String): Result<List<AlbumImageData>> = Result.success(emptyList())

        override suspend fun getImageCount(): Result<Int> = imageCountResult
    }
}
