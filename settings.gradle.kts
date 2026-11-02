rootProject.name = "InteractiveGameMap"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":genshinCompose")
include(":rdr2Compose")
include(":genshin")
include(":rdr2")

include(":feature:map:logic-api")
include(":feature:map:logic-genshin-impl")
include(":feature:map:logic-rdr2-impl")
include(":feature:map:ui-api")
include(":feature:map:ui-common")
include(":feature:map:ui-genshin-impl")
include(":feature:map:ui-rdr2-impl")
include(":feature:settings:ui-api")
include(":feature:settings:ui")

include(":core:analytics")
include(":core:database")
include(":core:navigation:api")
include(":core:navigation:impl")
include(":core:network")
include(":core:remoteconfig")
include(":core:settings")
include(":core:ui-core")
include(":core:ui")
include(":core:ui-genshin")
include(":core:ui-rdr2")
