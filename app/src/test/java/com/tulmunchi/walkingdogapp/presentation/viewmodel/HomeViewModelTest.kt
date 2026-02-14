package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.tulmunchi.walkingdogapp.domain.model.Dog
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `강아지 선택과 해제가 선택 텍스트를 갱신한다`() {
        val viewModel = HomeViewModel()

        viewModel.selectDog("Mongi", 5)
        viewModel.selectDog("Bori", 7)
        assertEquals("Mongi, Bori", viewModel.selectedDogsText.value)
        assertTrue(viewModel.hasSelectedDogs())

        viewModel.deselectDog("Mongi")
        assertEquals("Bori", viewModel.selectedDogsText.value)
        assertTrue(viewModel.hasSelectedDogs())

        viewModel.clearSelection()
        assertEquals("", viewModel.selectedDogsText.value)
        assertFalse(viewModel.hasSelectedDogs())
    }

    @Test
    fun `validateWalkStart는 상태에 맞는 결과를 반환한다`() {
        val viewModel = HomeViewModel()
        val dogs = listOf(Dog(name = "Mongi", weight = "5"))

        assertTrue(
            viewModel.validateWalkStart(
                dogsList = dogs,
                isDataLoaded = false,
                hasLocationPermission = true
            ) is WalkValidationResult.DataNotLoaded
        )

        assertTrue(
            viewModel.validateWalkStart(
                dogsList = emptyList(),
                isDataLoaded = true,
                hasLocationPermission = true
            ) is WalkValidationResult.NoDogRegistered
        )

        assertTrue(
            viewModel.validateWalkStart(
                dogsList = dogs,
                isDataLoaded = true,
                hasLocationPermission = true
            ) is WalkValidationResult.NoDogSelected
        )

        viewModel.selectDog("Mongi", 5)

        assertTrue(
            viewModel.validateWalkStart(
                dogsList = dogs,
                isDataLoaded = true,
                hasLocationPermission = false
            ) is WalkValidationResult.LocationPermissionDenied
        )

        assertTrue(
            viewModel.validateWalkStart(
                dogsList = dogs,
                isDataLoaded = true,
                hasLocationPermission = true
            ) is WalkValidationResult.Success
        )
    }
}
