plugins {
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.onthecrow.interactivegamemap.map"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.map.logicApi)
            implementation(projects.feature.map.logicCommon)
            implementation(projects.core.sync)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
