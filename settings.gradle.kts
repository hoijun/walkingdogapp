pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://jitpack.io")
        maven("https://naver.jfrog.io/artifactory/maven/")
        maven("https://devrepo.kakao.com/nexus/content/groups/public/")
        maven("https://repository.map.naver.com/archive/maven")
        mavenCentral()
    }
}

// 빌드 캐시 활성화 (빌드 속도 향상)
buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
    }
}

rootProject.name = "WalkingDogApp"
include(":app")
