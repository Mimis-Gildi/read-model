import org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: 100%.
 * No remainder prototyping slop.
 */


plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
}

group = "me.riddle.adventure.web.model"


kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    jvm()
    js {
        generateTypeScriptDefinitions()
        binaries.library()

        compilerOptions {
            target.set("es2015")
            sourceMap.set(true)
            sourceMapEmbedSources.set(SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
            moduleKind.set(MODULE_ES)
        }

        browser {
            testTask { useKarma { useChromeHeadless() } }
        }

    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
        }
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlin.logging)

        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(libs.slf4j.api)
            implementation(libs.logback.classic)
        }
    }
}

tasks {
    withType<Test>().configureEach {
        testLogging {
            showStandardStreams = true
            events("started", "passed", "skipped", "failed")
        }
    }
    withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
        testLogging {
            showStandardStreams = true
            showCauses = true
            events("started", "passed", "skipped", "failed", "standardOut", "standardError")
        }
    }
}
