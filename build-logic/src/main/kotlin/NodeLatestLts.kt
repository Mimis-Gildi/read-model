/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
import groovy.json.JsonSlurper
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.net.URI

abstract class NodeLatestLts : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String =
        (JsonSlurper().parse(URI("https://nodejs.org/dist/index.json").toURL()) as List<*>)
            .map { it as Map<*, *> }
            .first { it["lts"] != false }["version"].toString().removePrefix("v")
}
