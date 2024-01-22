import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") version "4.4.0" apply false
}

val properties = Properties().apply {
    load(FileInputStream("${rootDir}/local.properties"))
}
val navermapapiKey = properties["navermap_api_key"] ?: ""
val kakaoapikey = properties["kakaologin_api_key"] ?: ""


android {
    namespace = "com.example.walkingdogapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.walkingdogapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["NaverMap_API_KEY"] = navermapapiKey as String
        manifestPlaceholders["Kakao_API_KEY"]  = kakaoapikey as String
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        buildFeatures {
            viewBinding = true
            buildConfig = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.naver.maps:map-sdk:3.17.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.kakao.sdk:v2-user:2.19.0")
}