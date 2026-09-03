import org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: 100%.
 * No remaining prototyping slop.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "me.riddle.adventure.web.control"

kotlin {
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
        jsMain.dependencies {
            implementation(project(":model"))
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlin.logging)
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
