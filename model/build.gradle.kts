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
}

group = "me.riddle.adventure.web.model"

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    jvm()
    js { browser() }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlin.logging)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(libs.slf4j.api)
            implementation(libs.logback.classic)
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        showStandardStreams = true
        showCauses = true
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}
