import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun buildConfigString(value: String): String {
    val escapedValue = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escapedValue\""
}

android {
    namespace = "com.payda.iptv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.payda.iptv"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TEST_PLAYLIST_URL_1", buildConfigString(""))
        buildConfigField("String", "TEST_PLAYLIST_URL_2", buildConfigString(""))
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "TEST_PLAYLIST_URL_1",
                buildConfigString(localProperties.getProperty("TEST_PLAYLIST_URL_1").orEmpty())
            )
            buildConfigField(
                "String",
                "TEST_PLAYLIST_URL_2",
                buildConfigString(localProperties.getProperty("TEST_PLAYLIST_URL_2").orEmpty())
            )
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
