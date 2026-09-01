@file:Suppress("UnstableApiUsage")

/*
 * @rdd13r 2026-08-21
 */
rootProject.name = "read-model"

pluginManagement {

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version
                providers.gradleProperty("versionOfToolchainsFoojayResolver").get()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("ktor_libs")
            .from("io.ktor:ktor-version-catalog:${providers.gradleProperty("versionOfKtor").get()}")
    }
}

include("model", "service", "control", "kobweb")
