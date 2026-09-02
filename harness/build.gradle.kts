/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "me.riddle.adventure.web.harness"

kotlin {
    js {
        browser {
            commonWebpackConfig { outputFileName = "harness.js" }
        }
        compilerOptions {
            target.set("es2015")
        }
        binaries.executable()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":model"))
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
