/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
plugins {
    `kotlin-dsl`
}

dependencies {
    /* A precompiled script applies plugins by id only; the catalog's plugin is put on its classpath as a marker artifact. */
    implementation(libs.plugins.gradle.node.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    compileOnly(libs.plugins.kotlin.multiplatform.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
