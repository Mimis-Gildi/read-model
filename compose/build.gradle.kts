/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS


plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "me.riddle.adventure.web.compose"

kotlin {
    js {
        binaries.executable()

        compilerOptions {
            target.set("es2015")
            sourceMap.set(true)
            sourceMapEmbedSources.set(SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
            moduleKind.set(MODULE_ES)
        }

        browser()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)

            api(project(":model"))
            implementation(project(":harness"))
            implementation(project(":compose:core"))
        }
    }
}
