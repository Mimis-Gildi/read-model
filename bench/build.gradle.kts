/*
 * @rdd13r 2026-08-21
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "me.riddle.adventure.bench"

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)
}
