/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no prototyping slopremaining.
 * * @rdd13r 2026-08-21
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktor_libs.plugins.ktor)
}

group = "me.riddle.adventure.web.service"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}

/* The control plane page ships compiled.*/
tasks.processResources {
    from(project(":control").tasks.named("jsBrowserDistribution")) {
        include("control.js", "control.js.map")
        into("control")
    }
    from(project(":harness").tasks.named("jsBrowserDistribution")) {
        include("harness.js", "harness.js.map")
        into("harness")
    }
}


dependencies {
    implementation(project(":model"))

    implementation(ktor_libs.serialization.kotlinx.json)
    implementation(ktor_libs.server.cachingHeaders)
    implementation(ktor_libs.server.callLogging)
    implementation(ktor_libs.server.compression)
    implementation(ktor_libs.server.config.yaml)
    implementation(ktor_libs.server.contentNegotiation)
    implementation(ktor_libs.server.core)
    implementation(ktor_libs.server.cors)
    implementation(ktor_libs.server.metrics)
    implementation(ktor_libs.server.metrics.micrometer)
    implementation(ktor_libs.server.netty)
    implementation(ktor_libs.server.statusPages)
    implementation(ktor_libs.server.websockets)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    implementation(libs.micrometer.registry.prometheus)

    testImplementation(kotlin("test"))
    testImplementation(ktor_libs.server.testHost)
    testImplementation(ktor_libs.client.websockets)
}
