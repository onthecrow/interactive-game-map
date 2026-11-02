plugins {
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.onthecrow.interactivegamemap.analytics"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
    }
    // The navigation has a JVM target, its screens are tracked
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val archSlice = when (iosTarget.name) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator"
            else -> throw GradleException("Unknown iOS target architecture: ${iosTarget.name}")
        }

        // The app links the frameworks, the ones here are for the headers and the tests
        val groupDir = rootProject.file("lib/firebase/FirebaseAnalytics")
        val frameworks = groupDir.listFiles { file -> file.isDirectory && file.name.endsWith(".xcframework") }
            .orEmpty()
            .mapNotNull { xcFramework ->
                File(xcFramework, archSlice).takeIf { it.exists() }
                    ?.let { xcFramework.name.removeSuffix(".xcframework") to it.absolutePath }
            }

        iosTarget.compilations.getByName("main") {
            @Suppress("unused")
            val firebaseAnalytics by cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/firebaseAnalytics.def"))
                frameworks.forEach { (_, path) -> extraOpts("-compiler-option", "-F$path") }
            }
        }

        iosTarget.binaries.all {
            frameworks.forEach { (name, path) ->
                linkerOpts("-F$path", "-framework", name)
            }
            linkerOpts(
                "-lsqlite3", "-lz", "-lc++",
                "-framework", "StoreKit",
                "-framework", "Foundation",
                "-framework", "UIKit",
                "-framework", "SystemConfiguration",
                "-framework", "Security",
                "-framework", "AdSupport",
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
        }
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
