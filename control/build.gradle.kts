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
        browser {
            commonWebpackConfig { outputFileName = "control.js" }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":model"))
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlin.logging)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
