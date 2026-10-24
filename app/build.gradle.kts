plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// Production signing is enabled only by explicitly supplied CI/local environment secrets.
// Default GitHub preview bundles stay unsigned; never commit keystore files.
val uploadKeystoreFile = System.getenv("SHIFTORA_UPLOAD_KEYSTORE_FILE")
val uploadKeystorePassword = System.getenv("SHIFTORA_UPLOAD_KEYSTORE_PASSWORD")
val uploadKeyAlias = System.getenv("SHIFTORA_UPLOAD_KEY_ALIAS")
val uploadKeyPassword = System.getenv("SHIFTORA_UPLOAD_KEY_PASSWORD")
val usePlayUploadKey = listOf(uploadKeystoreFile, uploadKeystorePassword, uploadKeyAlias, uploadKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.brendigo.shiftora"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.brendigo.shiftora"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "0.9.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (usePlayUploadKey) {
            create("playUpload") {
                val filePath = file(uploadKeystoreFile!!)
                require(filePath.isFile) { "Upload keystore path is invalid." }
                storeFile = filePath
                storePassword = uploadKeystorePassword!!
                keyAlias = uploadKeyAlias!!
                keyPassword = uploadKeyPassword!!
            }
        }
    }
    buildTypes {
        release {
            if (usePlayUploadKey) signingConfig = signingConfigs.getByName("playUpload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
