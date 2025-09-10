plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
  namespace = "com.google.android.gms.example"
  compileSdk = 36

  defaultConfig {
    minSdk = 24

    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures { compose = true }
  composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_compiler") }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
}

dependencies {
  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
  implementation(platform("androidx.compose:compose-bom:2025.08.01"))
  implementation("androidx.compose.ui:ui:1.9.0")
  implementation("androidx.compose.ui:ui-graphics:1.9.0")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.foundation:foundation")
  implementation("com.google.android.gms:play-services-ads:24.5.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
  debugImplementation("androidx.compose.ui:ui-tooling")
}
