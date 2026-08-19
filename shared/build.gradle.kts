import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

compose.resources {
    packageOfResClass = "com.restart.spacestationtracker.shared.resources"
    publicResClass = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.restart.spacestationtracker.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ISSTrackerShared"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("io.ktor:ktor-client-core:3.4.1")
            implementation("io.ktor:ktor-client-content-negotiation:3.4.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.1")
            implementation("org.jetbrains.compose.runtime:runtime:1.9.3")
            implementation("org.jetbrains.compose.foundation:foundation:1.9.3")
            implementation("org.jetbrains.compose.ui:ui:1.9.3")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(compose.components.resources)
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.4.1")
            implementation("com.google.maps.android:maps-compose:8.3.0")
            implementation("com.google.android.gms:play-services-maps:20.0.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.4.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            implementation("io.ktor:ktor-client-mock:3.4.1")
        }
    }
}
