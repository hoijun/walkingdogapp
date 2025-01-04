import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

val properties = Properties().apply {
    load(FileInputStream("${rootDir}/local.properties"))
}
val navermapapiKey = properties["navermap_api_key"] ?: ""
val kakaoapikey = properties["kakaologin_api_key"] ?: ""
val kakaoredirecturi = properties["kakaologin_redirect_uri"] ?: ""
val naverclientid = properties["naverlogin_clientid"] ?: ""
val naverclientsecret = properties["naverlogin_clientsecret"] ?: ""



android {
    namespace = "com.tulmunchi.walkingdogapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tulmunchi.walkingdogapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["Kakao_Redirect_URI"] = kakaoredirecturi as String
        manifestPlaceholders["NaverMap_API_KEY"] = navermapapiKey as String
        manifestPlaceholders["Kakao_API_KEY"]  = kakaoapikey as String
        buildConfigField("String", "Kakao_API_KEY", kakaoapikey)
        buildConfigField("String", "Naver_ClientId", naverclientid as String)
        buildConfigField("String", "Naver_ClientSecret", naverclientsecret as String)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        buildFeatures {
            viewBinding = true
            dataBinding = true
            buildConfig = true
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
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
            resources.excludes.add("META-INF/LICENSE.md")
            resources.excludes.add("META-INF/LICENSE-notice.md")
        }
    }

    bundle {
        language {
            enableSplit = true
        }

        density {
            enableSplit = true
        }

        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")

    implementation("com.google.dagger:hilt-android:2.49")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage-ktx")

    // implementation("androidx.legacy:legacy-support-core-utils:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    implementation("com.kakao.sdk:v2-user:2.20.3")
    implementation("com.naver.maps:map-sdk:3.20.0")
    implementation(files("libs/oauth-5.9.0.aar"))

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("com.jakewharton.threetenabp:threetenabp:1.2.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    androidTestImplementation("androidx.test:monitor:1.7.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}