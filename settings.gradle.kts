pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Zatiaras POS"
include(":app")
include(":core:domain")
include(":core:ui")
include(":core:data")
include(":feature:auth")
include(":feature:home")
include(":feature:inventory")
include(":feature:pos")
include(":feature:reports")

