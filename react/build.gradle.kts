import com.github.gradle.node.npm.task.NpmTask

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * See build-logic/src/main/kotlin/read-model.node-fixture.gradle.kts for refactor justification.
 */
plugins {
    id("read-model.node-fixture")
}

group = "me.riddle.adventure.web.react"

val bundleVendor = tasks.register<NpmTask>("bundleVendor") {
    description = "Bundle React itself into the browser ESM the fixture page loads from /vendor/react"
    dependsOn(tasks.npmInstall)

    args.set(listOf("run", "vendor"))

    inputs.file("bundle-vendor.mjs")
    inputs.file("package.json")
    outputs.dir(layout.buildDirectory.dir("vendor/react"))
}

tasks.named("assembleTS") { dependsOn(bundleVendor) }
tasks.named("build") { dependsOn(bundleVendor) }
