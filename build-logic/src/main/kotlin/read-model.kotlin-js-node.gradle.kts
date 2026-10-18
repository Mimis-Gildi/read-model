import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
plugins.withType<NodeJsPlugin> {
    the<NodeJsEnvSpec>().version.set(providers.of(NodeLatestLts::class) {}.get())
}
