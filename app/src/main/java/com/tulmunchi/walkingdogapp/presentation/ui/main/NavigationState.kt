package com.tulmunchi.walkingdogapp.presentation.ui.main

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord

sealed class NavigationState {

    // BottomNavigation이 있는 화면들
    sealed class WithBottomNav : NavigationState() {
        object Home : WithBottomNav()
        object MyPage : WithBottomNav()
        object Collection : WithBottomNav()
        object AlbumMap : WithBottomNav()
    }

    // BottomNavigation이 없는 화면들 (전체 화면)
    sealed class WithoutBottomNav : NavigationState() {
        // Register 화면들
        data class RegisterDog(
            val dog: Dog? = null,              // null이면 새 등록, 값이 있으면 수정
            val from: String = "home"         // "home", "mypage", "manage", "doginfo:mypage", "doginfo:manage" - 뒤로가기 위치 판단용
        ) : WithoutBottomNav()

        data class RegisterUser(
            val from: String = "mypage"        // "mypage" - 뒤로가기 위치 판단용
        ) : WithoutBottomNav()

        // MyPage 하위 화면들
        object ManageDogs : WithoutBottomNav()

        data class DogInfo(
            val dog: Dog,
            val before: String  // "manage" 또는 "mypage" - 뒤로가기 위치 판단용
        ) : WithoutBottomNav()

        data class WalkInfo(
            val selectDateRecord: List<String>? = null,  // DetailWalkInfo에서 복귀 시만 사용
            val selectDog: Dog? = null                   // DetailWalkInfo에서 복귀 시만 사용
        ) : WithoutBottomNav()

        data class DetailWalkInfo(
            val walkRecord: WalkRecord,
            val dog: Dog
        ) : WithoutBottomNav()

        // Gallery 화면들
        object Gallery : WithoutBottomNav()

        data class DetailPicture(
            val selectImageIndex: Int  // 선택한 이미지 번호
        ) : WithoutBottomNav()

        // 기타
        object SettingAlarm : WithoutBottomNav()
        object Setting : WithoutBottomNav()
    }
}
