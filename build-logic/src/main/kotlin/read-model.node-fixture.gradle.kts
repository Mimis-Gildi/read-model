import com.github.gradle.node.npm.task.NpmTask

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * IMPORTANT: This exists because all TS fixtures build the same way.
 */
plugins {
    id("com.github.node-gradle.node")
}

node {
    download.set(true)
    version.set(providers.of(NodeLatestLts::class) {}.get())
}

val compileTS = tasks.register<NpmTask>("compileTS") {
    description = "Compile the TS fixture sources against the harness declarations and model injection."
    dependsOn(tasks.npmInstall, ":harness:jsBrowserProductionLibraryDistribution", ":model:jsBrowserProductionLibraryDistribution")

    args.set(listOf("run", "build"))

    // These are only to detect changes and rebuild
    inputs.dir("src/main/tsc")
    inputs.file("tsconfig.json")
    inputs.file("package.json")
    outputs.dir(layout.buildDirectory.dir("dist"))
}

val testTS = tasks.register<NpmTask>("testTS") {
    description = "Run the TS fixture test suite for the local unit tests."
    args.set(listOf("run", "test", "--", "--passWithNoTests"))
    dependsOn(compileTS)
}

val assembleTs = tasks.register("assembleTS") {
    group = "build"
    description = "Assembles the node artifacts."
    dependsOn(compileTS, testTS)
}


val cleanNpm = tasks.register<Delete>("cleanNpm") {
    description = "Deletes this module's specific garbage on global clean."
    delete("node_modules", "package-lock.json")
}

when (val defaultClean = tasks.findByName("clean")) {
    null -> tasks.register<Delete>("clean") {
        description = "Deletes this module's specific garbage on global clean."
        delete(layout.buildDirectory)
        dependsOn(cleanNpm)
    }

    else -> defaultClean.dependsOn(cleanNpm)
}


when (val defaultBuild = tasks.findByName("build")) {
    null -> tasks.register("build") {
        description = "Assembles and tests this project."
        group = "build"
        dependsOn(compileTS)
        outputs.files(compileTS.map { it.outputs.files })
    }

    else -> defaultBuild.dependsOn(compileTS)
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
