package com.tulmunchi.walkingdogapp.domain.model

/**
 * Domain-level error types
 */
sealed class DomainError : Throwable() {
    data class NetworkError(override val message: String = "네트워크 연결을 확인해주세요") : DomainError()
    data class ValidationError(override val message: String) : DomainError()
    data class AuthenticationError(override val message: String = "인증에 실패했습니다") : DomainError()
    data class NotFoundError(override val message: String = "데이터를 찾을 수 없습니다") : DomainError()
    data class UnknownError(override val message: String = "알 수 없는 오류가 발생했습니다") : DomainError()
    data class WeatherError(override val message: String = "날씨 정보를 가져오는데 실패했습니다") : DomainError()
}
