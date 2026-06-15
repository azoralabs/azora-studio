rootProject.name = "AzoraStudio"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":launcherApp:shared")
include(":launcherApp:androidApp")
include(":launcherApp:desktopApp")

include(":studioApp:shared")
include(":studioApp:androidApp")
include(":studioApp:desktopApp")

include(":build-config")

include(":azora-local:database")

include(":azora-sdk:canvas:domain")
include(":azora-sdk:canvas:presentation")

include(":azora-sdk:color:presentation")

include(":azora-sdk:docking:data")
include(":azora-sdk:docking:domain")
include(":azora-sdk:docking:presentation")

include(":azora-sdk-core:component")
include(":azora-sdk-core:data")
include(":azora-sdk-core:domain")
include(":azora-sdk-core:io")
include(":azora-sdk-core:presentation")
include(":azora-sdk-core:project:data")
include(":azora-sdk-core:project:domain")
include(":azora-sdk-core:project:presentation")
include(":azora-sdk-core:theme")
include(":azora-sdk-core:util")

include(":azora-sdk-plugin:core")
include(":azora-sdk-plugin:domain")
include(":azora-sdk-plugin:presentation")

include(":azora-shared")