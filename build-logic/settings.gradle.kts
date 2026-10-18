@file:Suppress("UnstableApiUsage")

/*
* Copyright 2026 @rdd13r (Vadim Kuhay)
* All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
*/
rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
