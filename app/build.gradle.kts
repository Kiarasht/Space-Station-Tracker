import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun propertyOrEnv(name: String): String? {
    return localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
}

fun String.toBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val n2yoApiKey = propertyOrEnv("N2YO_API_KEY").orEmpty()
val mapsApiKey = propertyOrEnv("MAPS_API_KEY").orEmpty()
val youtubeApiKey = propertyOrEnv("YOUTUBE_API_KEY").orEmpty()

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.restart.spacestationtracker"
    compileSdk = 36

    val versionProperties = Properties()
    val versionPropertiesFile = rootProject.file("version.properties")
    if (versionPropertiesFile.exists()) {
        versionProperties.load(FileInputStream(versionPropertiesFile))
    }

    defaultConfig {
        applicationId = "com.restart.spacestationtracker"
        minSdk = 24
        targetSdk = 36
        versionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull()
            ?: versionProperties.getProperty("APP_VERSION_CODE")?.toIntOrNull()
            ?: 1
        versionName = System.getenv("ANDROID_VERSION_NAME")?.takeIf { it.isNotBlank() }
            ?: versionProperties.getProperty("APP_VERSION_NAME")?.takeIf { it.isNotBlank() }
            ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "YOUTUBE_API_KEY", youtubeApiKey.toBuildConfigString())
        buildConfigField("String", "N2YO_API_KEY", n2yoApiKey.toBuildConfigString())

        val youtubeLiveStreamsUrl = localProperties.getProperty("YOUTUBE_LIVE_STREAMS_URL")
            ?: "https://raw.githubusercontent.com/Kiarasht/Space-Station-Tracker/live-stream-cache/docs/nasa-live-streams.json"
        buildConfigField(
            "String",
            "YOUTUBE_LIVE_STREAMS_URL",
            youtubeLiveStreamsUrl.toBuildConfigString()
        )

        resValue("string", "google_maps_key", mapsApiKey)

        val useAdMobTestAds = localProperties.getProperty("USE_ADMOB_TEST_ADS")
            ?.trim()
            ?.uppercase()
            ?.let { value -> value == "1" || value == "YES" || value == "TRUE" }
            ?: false
        buildConfigField("boolean", "USE_ADMOB_TEST_ADS", useAdMobTestAds.toString())

        val umpDebugGeography = localProperties.getProperty("UMP_DEBUG_GEOGRAPHY") ?: ""
        val umpDebugDeviceIds = localProperties.getProperty("UMP_DEBUG_DEVICE_IDS") ?: ""
        buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", umpDebugGeography.toBuildConfigString())
        buildConfigField("String", "UMP_DEBUG_DEVICE_IDS", umpDebugDeviceIds.toBuildConfigString())
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

gradle.taskGraph.whenReady {
    val includesAndroidReleaseBuild = allTasks.any { task ->
        task.project == project && task.name.contains("Release", ignoreCase = true)
    }
    if (includesAndroidReleaseBuild && n2yoApiKey.isBlank()) {
        throw org.gradle.api.GradleException(
            "N2YO_API_KEY must be set in local.properties or the environment for release builds."
        )
    }
    if (includesAndroidReleaseBuild && mapsApiKey.isBlank()) {
        throw org.gradle.api.GradleException(
            "MAPS_API_KEY must be set in local.properties or the environment for release builds."
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    // Core & Compose
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("com.google.android.material:material:1.13.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Maps
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Ads & User Messaging Platform
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-ads:25.2.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // Settings
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.android.billingclient:billing:9.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
