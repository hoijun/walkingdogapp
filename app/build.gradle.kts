import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

fun Properties.readConfig(propertyKey: String, envKey: String): String? {
    return getProperty(propertyKey)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.removeSurrounding("\"")
        ?: System.getenv(envKey)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}

fun String.toBuildConfigStringLiteral(): String {
    val escaped = replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

val properties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val naverMapApiKey = properties.readConfig("navermap_api_key", "NAVERMAP_API_KEY").orEmpty()
val kakaoApiKey = properties.readConfig("kakaologin_api_key", "KAKAOLOGIN_API_KEY").orEmpty()
val kakaoRedirectUri = properties.readConfig("kakaologin_redirect_uri", "KAKAOLOGIN_REDIRECT_URI").orEmpty()
val naverClientId = properties.readConfig("naverlogin_clientid", "NAVERLOGIN_CLIENTID").orEmpty()
val naverClientSecret = properties.readConfig("naverlogin_clientsecret", "NAVERLOGIN_CLIENTSECRET").orEmpty()
val weatherApiKey = properties.readConfig("weather_api_key", "WEATHER_API_KEY").orEmpty()

val keystoreFile = properties.readConfig("keystore.file", "ANDROID_KEYSTORE_FILE")
val keystorePassword = properties.readConfig("keystore.password", "ANDROID_KEYSTORE_PASSWORD")
val keystoreKeyAlias = properties.readConfig("keystore.key.alias", "ANDROID_KEY_ALIAS")
val keystoreKeyPassword = properties.readConfig("keystore.key.password", "ANDROID_KEY_PASSWORD")
val hasReleaseSigning = keystoreFile != null &&
    keystorePassword != null &&
    keystoreKeyAlias != null &&
    keystoreKeyPassword != null

android {
    signingConfigs {
        if (hasReleaseSigning) {
            val releaseKeystoreFile = requireNotNull(keystoreFile)
            val releaseKeystorePassword = requireNotNull(keystorePassword)
            val releaseKeyAlias = requireNotNull(keystoreKeyAlias)
            val releaseKeyPassword = requireNotNull(keystoreKeyPassword)
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    namespace = "com.tulmunchi.walkingdogapp"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "com.tulmunchi.walkingdogapp"
        minSdk = 27
        targetSdk = 36
        versionCode = 13
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["Kakao_Redirect_URI"] = kakaoRedirectUri
        manifestPlaceholders["Kakao_API_KEY"] = kakaoApiKey
        buildConfigField("String", "NaverMap_API_KEY", naverMapApiKey.toBuildConfigStringLiteral())
        buildConfigField("String", "Kakao_API_KEY", kakaoApiKey.toBuildConfigStringLiteral())
        buildConfigField("String", "Naver_ClientId", naverClientId.toBuildConfigStringLiteral())
        buildConfigField("String", "Naver_ClientSecret", naverClientSecret.toBuildConfigStringLiteral())
        buildConfigField("String", "Weather_API_KEY", weatherApiKey.toBuildConfigStringLiteral())

        if (hasReleaseSigning) {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildTypes {
        debug {
            extra.set("enableCrashlytics", false)
            extra.set("alwaysUpdateBuildId", false)
            // Debug 빌드 속도 최적화
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            splits {
                abi.isEnable = false
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // signing 정보가 없으면 debug signing 사용
            if (!hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // 증분 컴파일 최적화
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "17"
        // Kotlin 컴파일 최적화
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
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

    // Lint 검사 최적화 (빌드 속도 향상)
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")

    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.2.0")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // implementation("androidx.legacy:legacy-support-core-utils:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("com.kakao.sdk:v2-user:2.23.0")
    implementation("com.naver.maps:map-sdk:3.22.1")
    implementation("com.navercorp.nid:oauth:5.10.0")

    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.9")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    androidTestImplementation("androidx.test:monitor:1.7.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation(kotlin("test"))
}
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*_Factory*",
                    "*_MembersInjector*",
                    "*Hilt*",
                    "*Dagger*",
                    "*Binding",
                    "*BindingImpl",
                    "*DataBinderMapperImpl",
                    "*Directions",
                    "*Args"
                )
                packages(
                    "dagger.hilt.internal",
                    "hilt_aggregated_deps"
                )
            }
        }
        verify {
            rule {
                minBound(8)
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = true
    config.setFrom(rootProject.file("detekt.yml"))
    basePath = rootDir.absolutePath
    source.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
            "src/debug/java",
            "src/debug/kotlin"
        )
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    exclude(
        "**/build/**",
        "**/generated/**",
        "**/resources/**",
        "**/tmp/**",
        "**/test/**",
        "**/androidTest/**"
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
    }
}
