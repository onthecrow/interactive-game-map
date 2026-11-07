import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    dependencies {
        implementation(projects.genshinCompose)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.core.splashscreen)
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val mapsKey = (System.getenv("GENSHIN_MAPS_API_KEY")
    ?: localProps.getProperty("GENSHIN_MAPS_API_KEY")
    ?: "")

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.onthecrow.genshinmap"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.onthecrow.genshinmap"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"

        resValue("string", "google_maps_key", mapsKey)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (signingProps.containsKey("genshin.storeFile")) {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("genshin.storeFile"))
                storePassword = signingProps.getProperty("genshin.storePassword")
                keyAlias = signingProps.getProperty("genshin.keyAlias")
                keyPassword = signingProps.getProperty("genshin.keyPassword")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["firebaseCollectionEnabled"] = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            manifestPlaceholders["firebaseCollectionEnabled"] = true
        }
    }
    buildFeatures {
        resValues = true
    }
}

// Compose tooling (previews on a device, the layout inspector) only in debug builds
dependencies {
    debugImplementation(libs.compose.uiTooling)
}
