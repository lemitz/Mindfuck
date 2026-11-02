plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// One version for everything: the VERSION file in the repository root (also read by the release
// workflow for the tag). versionCode = major * 10000 + minor * 100 + patch.
val appVersion = rootProject.file("VERSION").readText().trim()
require(Regex("""\d+\.\d+\.\d+""").matches(appVersion)) { "VERSION must look like 1.2.3, got '$appVersion'" }
val versionParts = appVersion.split('.').map { it.toInt() }

// Release signing comes from the environment (CI secrets); see scripts/make-keystore.ps1.
// Without it the release APK is signed with the local debug key so it still installs.
val keystorePath: String? = System.getenv("MINDFUCK_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.mindfuck.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mindfuck.app"
        minSdk = 34 // Android 14+
        targetSdk = 36
        versionCode = versionParts[0] * 10000 + versionParts[1] * 100 + versionParts[2]
        versionName = appVersion
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MINDFUCK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MINDFUCK_KEY_ALIAS")
                keyPassword = System.getenv("MINDFUCK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (keystorePath != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        noCompress += "ttf"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
