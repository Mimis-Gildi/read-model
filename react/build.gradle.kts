import com.github.gradle.node.npm.task.NpmTask

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
plugins {
    alias(libs.plugins.gradle.node)
}

group = "me.riddle.adventure.web.react"

node {
    download = false                                                                            // Set to true when not own
    version = libs.versions.node.get()                                                          // 24.20.0 local and in catalog

    nodeProjectDir.set(file(projectDir))
    npmCommand.set("${System.getProperty("user.home")}/.volta/bin/npm")                         // Comment out when not own
}


val compileTS = tasks.register<NpmTask>("compileTS") {
    description = "Compile the React fixture against the harness declarations"
    dependsOn(tasks.npmInstall, ":harness:jsBrowserProductionLibraryDistribution")

    args.set(listOf("run", "build"))

    // These are only to detect changes and rebuild
    inputs.dir("src/main/tsc")
    inputs.file("tsconfig.json")
    inputs.file("package.json")
    outputs.dir(layout.buildDirectory.dir("dist"))
}

val bundleVendor = tasks.register<NpmTask>("bundleVendor") {
    description = "Bundle React itself into the browser ESM the fixture page loads from /vendor/react"
    dependsOn(tasks.npmInstall)

    args.set(listOf("run", "vendor"))

    inputs.file("bundle-vendor.mjs")
    inputs.file("package.json")
    outputs.dir(layout.buildDirectory.dir("vendor/react"))
}

val testTS = tasks.register<NpmTask>("testTS") {
    description = "Run the React fixture test suite"
    args.set(listOf("run", "test", "--", "--passWithNoTests"))
    dependsOn(compileTS)
}

val assembleTs = tasks.register("assembleTS") {
    group = "build"
    description = "Assembles the node artifacts."
    dependsOn(compileTS, bundleVendor, testTS)
}


val cleanNpm = tasks.register<Delete>("cleanNpm") {
    description = "Deletes this modules specific garbage on global clean."
    delete("node_modules", "package-lock.json")
}

when (val defaultClean = tasks.findByName("clean")) {
    null -> tasks.register<Delete>("clean") {
        description = "Deletes this modules specific garbage on global clean."
        delete(layout.buildDirectory)
        dependsOn(cleanNpm)
    }
    else -> defaultClean.dependsOn(cleanNpm)
}


when (val defaultBuild = tasks.findByName("build")) {
    null -> tasks.register("build") {
        description = "Assembles and tests this project."
        group = "build"
        dependsOn(compileTS, bundleVendor)
        outputs.files(compileTS.map { it.outputs.files })
    }
    else -> defaultBuild.dependsOn(compileTS, bundleVendor)
}

when (val defaultAssemble = tasks.findByName("assemble")) {
    null -> tasks.register("assemble") {
        description = "Assembles and tests this project."
        group = "build"
        dependsOn(assembleTs)
    }
    else -> defaultAssemble.dependsOn(assembleTs)
}

when (val defaultCheck = tasks.findByName("check")) {
    null -> tasks.register("check") {
        description = "Assembles and tests this project."
        group = "verification"
        dependsOn(testTS)
    }
    else -> defaultCheck.dependsOn(testTS)
}
