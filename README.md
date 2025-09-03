# 🐕 강아지 산책 앱 (Walking Dog App)

  [![Kotlin](https://img.shields.io/badge/kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Android](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
  [![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
  [![Google Play](https://img.shields.io/badge/Google%20Play-Download-blue)](https://play.google.com/store/apps/details?id=com.tulmunchi.walkingdogapp&pcampaignid=web_share)

  강아지와 함께 산책하면서 다양한 캐릭터 아이콘을 획득하여 도감을 채워나가는 강아지 산책 앱입니다.

  ## ✨ 주요 기능

  - **🚶‍♀️ 산책 기록 관리**: 강아지와의 산책 정보를 기록하고 확인
  - **🏆 도감 시스템**: 산책 중 캐릭터 아이콘 획득으로 도감 완성
  - **📍 지도 연동**: 산책 시 촬영한 사진을 지도에서 확인 가능
  - **⏰ 알람 기능**: 산책 시간 알림 설정
  - **📸 갤러리**: 산책 중 촬영한 사진들을 체계적으로 관리

  ## 🛠 기술 스택

  ### 개발 환경
  - **언어**: Kotlin
  - **SDK**: Android API 26+ (Target: API 35)
  - **IDE**: Android Studio
  - **빌드 도구**: Gradle (KTS)

  ### 주요 라이브러리
  | 카테고리 | 라이브러리 | 용도 |
  |----------|------------|------|
  | **Backend** | Firebase | 데이터베이스, 인증, 분석, 크래시 리포팅 |
  | **Location** | FusedLocationProviderClient | GPS 위치 추적 |
  | **Maps** | Naver Map SDK | 지도 서비스 |
  | **Database** | Room | 로컬 데이터베이스 (알람 설정 등) |
  | **Async** | Coroutines | 비동기 처리 |
  | **DI** | Dagger Hilt | 의존성 주입 |
  | **UI** | Material Design, Lottie | 사용자 인터페이스 |
  | **Auth** | Kakao SDK, Naver OAuth | 소셜 로그인 |
  | **Image** | Glide | 이미지 로딩 |

  ### 아키텍처
  - **패턴**: MVVM (Model-View-ViewModel)
  - **구조**: Multiple Activity, Multiple Fragment
  - **의존성 주입**: Dagger Hilt
  - **데이터 바인딩**: Android Data Binding

  ## 🚀 시작하기

  ### 요구사항
  - Android SDK 26 이상
  - JDK 17

  - API 키 설정
  local.properties 파일에 다음 키들을 추가:
  navermap_api_key="YOUR_NAVER_MAP_API_KEY"
  kakaologin_api_key="YOUR_KAKAO_API_KEY"
  kakaologin_redirect_uri="YOUR_KAKAO_REDIRECT_URI"
  naverlogin_clientid="YOUR_NAVER_CLIENT_ID"
  naverlogin_clientsecret="YOUR_NAVER_CLIENT_SECRET"

  - Firebase 설정
    - Firebase 프로젝트 생성
    - google-services.json 파일을 app/ 디렉토리에 추가

  ## 📊 프로젝트 정보

  - 개발 기간: 2024년 1월 ~ 2024년 12월
  - 개발 인원:
     - 안드로이드 개발자 1명
     - 디자인 2명

  ## 📄 라이선스 및 저작권

  이미지 저작권

  - 산책 아이콘: https://kor.pngtree.com/freepng/yellow-white-pixel-kuki-dog_6357944.html - 百事可口阔落 작가

  ## 📱 플레이 스토어 링크

  - https://play.google.com/store/apps/details?id=com.tulmunchi.walkingdogapp&pcampaignid=web_share
