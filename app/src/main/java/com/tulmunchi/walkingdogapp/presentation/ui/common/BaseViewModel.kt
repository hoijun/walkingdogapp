package com.tulmunchi.walkingdogapp.presentation.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tulmunchi.walkingdogapp.domain.model.DomainError

/**
 * Base ViewModel for common functionality across all ViewModels
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * Loading state
     */
    protected val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    /**
     * Error message
     */
    protected val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    /**
     * Handle Result with automatic error processing
     * @param onSuccess Callback for successful result
     * @param onError Optional callback for error handling
     */
    protected fun <T> Result<T>.handle(
        onSuccess: (T) -> Unit,
        onError: ((Throwable) -> Unit)? = null
    ) {
        fold(
            onSuccess = onSuccess,
            onFailure = { throwable ->
                val message = when (throwable) {
                    is DomainError.NetworkError -> "네트워크 연결을 확인해주세요"
                    is DomainError.ValidationError -> throwable.message
                    is DomainError.UnknownError -> throwable.message
                    else -> "오류가 발생했습니다"
                }
                _error.postValue(message)
                onError?.invoke(throwable)
            }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
