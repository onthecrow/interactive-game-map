plugins {
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.onthecrow.interactivegamemap.navigation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3)
            implementation(projects.core.navigation.api)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.koin.core)
        }
    }
}
