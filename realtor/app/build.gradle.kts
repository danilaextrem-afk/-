plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
val releaseCredentials = listOf(
    "REALTOR_SIGNING_STORE", "REALTOR_SIGNING_STORE_PASSWORD",
    "REALTOR_SIGNING_ALIAS", "REALTOR_SIGNING_KEY_PASSWORD"
).associateWith { providers.environmentVariable(it).orNull }
require(releaseCredentials.values.all { it.isNullOrBlank() } || releaseCredentials.values.none { it.isNullOrBlank() }) {
    "Production signing requires all four REALTOR_SIGNING_* settings."
}

android {
    namespace = "ru.vladis.realtorai"
    compileSdk = 35
    defaultConfig {
        applicationId = if (providers.gradleProperty("sideBySide").isPresent) "ru.vladis.realtorai.workspace" else "ru.vladis.realtorai"
        minSdk = 26; targetSdk = 35
        versionCode = 27; versionName = "1.0-rc9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += "arm64-v8a"
            if (providers.gradleProperty("emulatorAbi").isPresent) abiFilters += "x86_64"
        }
    }
    signingConfigs {
        if (!releaseCredentials.getValue("REALTOR_SIGNING_STORE").isNullOrBlank()) {
            create("production") {
                storeFile = file(requireNotNull(releaseCredentials.getValue("REALTOR_SIGNING_STORE")))
                storePassword = releaseCredentials.getValue("REALTOR_SIGNING_STORE_PASSWORD")
                keyAlias = releaseCredentials.getValue("REALTOR_SIGNING_ALIAS")
                keyPassword = releaseCredentials.getValue("REALTOR_SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        val release = getByName("release")
        release.signingConfig = signingConfigs.findByName("production")
        create("preview") {
            initWith(release)
            applicationIdSuffix = ".preview9"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    ndkVersion = "27.2.12479018"
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mediapipe:tasks-genai:0.10.27")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    testImplementation("junit:junit:4.13.2")
}
