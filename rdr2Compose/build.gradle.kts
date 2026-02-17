plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.onthecrow.nomadrates"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }

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
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(projects.core.navigation.api)
            implementation(projects.core.navigation.impl)
            implementation(projects.feature.map.uiApi)
            implementation(projects.feature.map.uiImpl)

            implementation(projects.feature.map.uiImpl)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
