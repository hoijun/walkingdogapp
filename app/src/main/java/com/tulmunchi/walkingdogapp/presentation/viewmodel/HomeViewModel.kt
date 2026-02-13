package com.tulmunchi.walkingdogapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tulmunchi.walkingdogapp.domain.model.Dog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for HomeFragment
 * Manages selected dogs state
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {

    private val _selectedDogs = MutableLiveData<List<String>>(emptyList())

    private val _selectedDogWeights = MutableLiveData<Map<String, Int>>(emptyMap())

    private val _selectedDogsText = MutableLiveData("")
    val selectedDogsText: LiveData<String> get() = _selectedDogsText

    /**
     * Select a dog for walking
     */
    fun selectDog(name: String, weight: Int) {
        val currentDogs = _selectedDogs.value.orEmpty().toMutableList()
        val currentWeights = _selectedDogWeights.value.orEmpty().toMutableMap()

        if (!currentDogs.contains(name)) {
            currentDogs.add(name)
            currentWeights[name] = weight

            _selectedDogs.value = currentDogs
            _selectedDogWeights.value = currentWeights
            updateSelectedDogsText()
        }
    }

    /**
     * Deselect a dog
     */
    fun deselectDog(name: String) {
        val currentDogs = _selectedDogs.value.orEmpty().toMutableList()
        val currentWeights = _selectedDogWeights.value.orEmpty().toMutableMap()

        if (currentDogs.contains(name)) {
            currentDogs.remove(name)
            currentWeights.remove(name)

            _selectedDogs.value = currentDogs
            _selectedDogWeights.value = currentWeights
            updateSelectedDogsText()
        }
    }

    /**
     * Toggle dog selection
     */
    fun toggleDogSelection(name: String, weight: Int) {
        if (_selectedDogs.value.orEmpty().contains(name)) {
            deselectDog(name)
        } else {
            selectDog(name, weight)
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _selectedDogs.value = emptyList()
        _selectedDogWeights.value = emptyMap()
        updateSelectedDogsText()
    }

    /**
     * Update selected dogs text display
     */
    private fun updateSelectedDogsText() {
        _selectedDogsText.value = _selectedDogs.value.orEmpty().joinToString(", ")
    }

    /**
     * Check if any dog is selected
     */
    fun hasSelectedDogs(): Boolean {
        return _selectedDogs.value.orEmpty().isNotEmpty()
    }

    /**
     * Validate if walk can start
     * @param dogsList List of registered dogs
     * @param isDataLoaded Whether data is successfully loaded
     * @param hasLocationPermission Whether location permission is granted
     * @return WalkValidationResult
     */
    fun validateWalkStart(
        dogsList: List<Dog>,
        isDataLoaded: Boolean,
        hasLocationPermission: Boolean
    ): WalkValidationResult {
        // Check if data is loaded
        if (!isDataLoaded) {
            return WalkValidationResult.DataNotLoaded
        }

        // Check if there are registered dogs
        if (dogsList.isEmpty()) {
            return WalkValidationResult.NoDogRegistered
        }

        // Check if any dog is selected
        if (!hasSelectedDogs()) {
            return WalkValidationResult.NoDogSelected
        }

        // Check location permissions
        if (!hasLocationPermission) {
            return WalkValidationResult.LocationPermissionDenied
        }

        return WalkValidationResult.Success
    }

    /**
     * Get selected dog names as ArrayList
     */
    fun getSelectedDogNames(): ArrayList<String> {
        return ArrayList(_selectedDogs.value.orEmpty())
    }

    /**
     * Get selected dog weights as ArrayList
     */
    fun getSelectedDogWeights(): ArrayList<Int> {
        return ArrayList(_selectedDogs.value.orEmpty().map {
            _selectedDogWeights.value?.get(it) ?: 0
        })
    }
}

/**
 * Result of walk validation
 */
sealed class WalkValidationResult {
    object Success : WalkValidationResult()
    object NetworkUnavailable : WalkValidationResult()
    object DataNotLoaded : WalkValidationResult()
    object NoDogRegistered : WalkValidationResult()
    object NoDogSelected : WalkValidationResult()
    object LocationPermissionDenied : WalkValidationResult()
}
