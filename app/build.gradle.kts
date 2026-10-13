import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: the GitHub release workflow writes keystore.properties (never committed)
// from the repository secrets before building:
//   storeFile=/path/to/release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

/** The checkout described from the latest v* tag, or "0.0.0-dev" without tags or git. */
fun gitVersion(): String =
    runCatching {
        providers.exec {
            commandLine("git", "describe", "--tags", "--match", "v[0-9]*", "--dirty")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim().removePrefix("v")
    }.getOrNull()?.ifBlank { null } ?: "0.0.0-dev"

android {
    namespace = "io.github.impca201.snoozer"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.impca201.snoozer"
        minSdk = 26
        targetSdk = 37
        // Releases: the release workflow passes -PversionName (from the tag) and -PversionCode
        // (major * 10000 + minor * 100 + patch). Every other build describes its checkout instead
        // (e.g. "1.0.0-3-gabc1234"), so a development build never looks like a release.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse(gitVersion())
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        localeFilters += listOf("en", "nl", "fr", "de", "es", "it")
        // Lists the languages for Android 13+'s per-app language setting.
        generateLocaleConfig = true
    }

    dependenciesInfo {
        // Not needed outside Google Play; keeps the APK free of an encrypted dependency blob.
        includeInApk = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.graphics.shapes)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
