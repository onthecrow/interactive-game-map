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

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val libsDir = rootProject.file("lib/maps")

        val xcFramework = File(libsDir, "GoogleMaps.xcframework")

        val archSlice = when (iosTarget.name) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator"
            else -> error("Unknown iOS target: ${iosTarget.name}")
        }

        val sliceDir = File(xcFramework, archSlice)
        val headersDir = File(sliceDir, "Headers")
        val headersGoogleMapsDir = File(headersDir, "GoogleMaps")
        val staticLib = File(sliceDir, "GoogleMaps.a")

        require(headersDir.exists()) { "Headers not found: $headersDir" }
        require(staticLib.exists()) { "GoogleMaps.a not found: $staticLib" }

        iosTarget.compilations.getByName("main") {
            val googleMaps by cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/googlemaps.def"))

                extraOpts("-compiler-option", "-I${headersDir.absolutePath}")
                extraOpts("-compiler-option", "-I${headersGoogleMapsDir.absolutePath}")
            }
        }

        iosTarget.binaries.all {
            linkerOpts(staticLib.absolutePath)

            linkerOpts("-ObjC")
            linkerOpts(
                "-lz", "-lc++",
                "-framework", "Accelerate",
                "-framework", "Contacts",
                "-framework", "CoreData",
                "-framework", "CoreGraphics",
                "-framework", "CoreLocation",
                "-framework", "CoreImage",
                "-framework", "CoreTelephony",
                "-framework", "CoreText",
                "-framework", "GLKit",
                "-framework", "ImageIO",
                "-framework", "Metal",
                "-framework", "MetricKit",
                "-framework", "QuartzCore",
                "-framework", "Security",
                "-framework", "SystemConfiguration",
                "-framework", "UIKit",
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.services.maps)
        }
        commonMain.dependencies {
            implementation(projects.core.analytics)
            implementation(projects.core.navigation.api)
            implementation(projects.core.remoteconfig)
            implementation(projects.core.settings)
            implementation(projects.core.ui)
            implementation(projects.core.uiCore)
            implementation(projects.feature.map.uiApi)
            implementation(projects.feature.map.logicApi)
            implementation(projects.feature.settings.uiApi)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Compose previews of this module in Android Studio, kept out of the apps that use it
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
