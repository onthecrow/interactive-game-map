import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    dependencies {
        implementation(projects.genshinCompose)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.activity.compose)
    }
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val mapsKey = (System.getenv("GENSHIN_MAPS_API_KEY")
    ?: localProps.getProperty("GENSHIN_MAPS_API_KEY")
    ?: "")

android {
    namespace = "com.onthecrow.genshinmap"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.onthecrow.genshinmap"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        resValue("string", "google_maps_key", mapsKey)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        resValues = true
    }
}
