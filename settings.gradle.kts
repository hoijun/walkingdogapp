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

rootProject.name = "WalkingDogApp"
include(":app")
