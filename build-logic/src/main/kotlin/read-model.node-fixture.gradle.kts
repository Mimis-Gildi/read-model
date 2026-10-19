import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * IMPORTANT: This exists because all TS fixtures build the same way.
 */
plugins {
    base
    id("com.github.node-gradle.node")
}

node {
    download.set(true)
    version.set(providers.of(NodeLatestLts::class) {}.get())
}

val stageTestSetup = tasks.register("stageTestSetup") {
    description = "Stage the node:test setup where its imports resolve against this fixture's node_modules."
    val sources = listOf("setup.ts", "setup.d.ts")
        .associateWith { NodeLatestLts::class.java.getResource("/read-model/test/$it")!!.readText() }
    val staged = layout.buildDirectory.dir("tmp/test")

    inputs.properties(sources)
    outputs.dir(staged)
    doLast { sources.forEach { (name, source) -> staged.get().file(name).asFile.writeText(source) } }
}

val compileTS = tasks.register<NpmTask>("compileTS") {
    description = "Compile the TS fixture sources against the harness declarations and model injection."
    dependsOn(tasks.npmInstall, stageTestSetup, ":harness:jsBrowserProductionLibraryDistribution", ":model:jsBrowserProductionLibraryDistribution")

    args.set(listOf("run", "build"))

    // These are only to detect changes and rebuild
    inputs.dir("src/main/tsc")
    inputs.file("tsconfig.json")
    inputs.file("package.json")
    outputs.dir(layout.buildDirectory.dir("dist"))
}

val testTS = tasks.register<NpmTask>("testTS") {
    description = "Run the TS fixture test suite for the local unit tests."
    args.set(listOf("run", "test"))
    dependsOn(compileTS)
}

val stageVendor = tasks.register("stageVendor") {
    description = "Stage the vendor bundler where its imports resolve against this fixture's node_modules."
    val source = NodeLatestLts::class.java.getResource("/read-model/bundle-vendor.mjs")!!.readText()
    val staged = layout.buildDirectory.file("tmp/vendor/bundle-vendor.mjs")

    inputs.property("source", source)
    outputs.file(staged)
    doLast { staged.get().asFile.writeText(source) }
}

val bundleVendor = tasks.register<NodeTask>("bundleVendor") {
    description = "Bundle the modules package.json declares under \"vendor\" into the browser ESM the fixture page loads from /vendor/<fixture>"
    dependsOn(tasks.npmInstall)
    val out = layout.buildDirectory.dir("vendor/${project.name}")

    script.fileProvider(stageVendor.map { it.outputs.files.singleFile })
    args.set(listOf(out.get().asFile.absolutePath))

    inputs.file("package.json")
    outputs.dir(out)
}

val assembleTs = tasks.register("assembleTS") {
    group = "build"
    description = "Assembles the node artifacts."
    dependsOn(compileTS, testTS, bundleVendor)
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
        dependsOn(compileTS, bundleVendor)
        outputs.files(compileTS.map { it.outputs.files })
    }

    else -> defaultBuild.apply {
        dependsOn(compileTS, bundleVendor)
        outputs.files(compileTS.map { it.outputs.files })
    }
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

when (val defaultTest = tasks.findByName("test")) {
    null -> tasks.register("test") {
        description = "Runs the TS fixture test suite."
        group = "verification"
        dependsOn(testTS)
    }

    else -> defaultTest.dependsOn(testTS)
}
