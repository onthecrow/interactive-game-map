import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    dependencies {
        implementation(projects.rdr2Compose)
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

val mapsKey = (System.getenv("RDR2_MAPS_API_KEY")
    ?: localProps.getProperty("RDR2_MAPS_API_KEY")
    ?: "")

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.onthecrow.mapslinger"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.onthecrow.mapslinger"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        resValue("string", "google_maps_key", mapsKey)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (signingProps.containsKey("rdr2.storeFile")) {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("rdr2.storeFile"))
                storePassword = signingProps.getProperty("rdr2.storePassword")
                keyAlias = signingProps.getProperty("rdr2.keyAlias")
                keyPassword = signingProps.getProperty("rdr2.keyPassword")
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
