/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 *  ToDo: remap /ws
 *  ToDo: figure actual comms channels
 *  FixMe: think about Full-App fixtures: throw away servers or host and map
 */
package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.bench.Dataset
import me.riddle.adventure.web.model.bench.Level
import me.riddle.adventure.web.model.status.Module
import me.riddle.adventure.web.model.status.Report
import me.riddle.adventure.web.service.data.service.HumanResourcesDataService
import me.riddle.adventure.web.service.data.status.PerformanceScoreTable

private val logger = KotlinLogging.logger {}

/** One instance for the whole application; it generates on the first request to memoize thereafter. */
private val humanResources = HumanResourcesDataService()

fun Application.configureRouting() {

    routing {

//        ToDo: remap /ws
        webSocket("/ws") {
            PerformanceScoreTable.join(this)
            try {
                for (frame in incoming) {
                    logger.debug { "Received frame: $frame" }
                    when (frame) {
                        is Frame.Text -> frame.readText().let { text ->
                            runCatching { Json.decodeFromString<Report>(text) }
                                .onSuccess { report ->
                                    PerformanceScoreTable.record(report)
                                    logger.debug { "Recorded report for ${report.uiFramework}->${report.datasetKey}->${report.rung} (${report.runId})" }
                                }
                                .onFailure { error ->
                                    logger.warn(error) { "Unreadable frame: $text" }
                                }
                        }

                        else -> logger.warn { "Ignored non-text frame: $frame" }
                    }
                }
            } finally {
                PerformanceScoreTable.leave(this)
            }
        }

        get("/data/{datasetKey}") {
            when (val dataset = Dataset.of(call.parameters["datasetKey"])) {
                null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["datasetKey"]}")
                else -> call.respond(humanResources.get(dataset))
            }
        }
// FixMe: see if constants can be used

        get("/data/{datasetKey}/{level}") {
            val dataset = Dataset.of(call.parameters["datasetKey"])
            val level = call.parameters["level"]?.toIntOrNull()?.let(Level::at)
            when {
                dataset == null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["datasetKey"]}")
                level == null -> call.respond(HttpStatusCode.NotFound, "No such level: ${call.parameters["level"]}")
                else -> call.respond(humanResources.get(dataset, level))
            }
        }

//        FixMe: What's the best way to map fixtures, especially ones with own server even if not used
        Module.entries.forEach { staticResources("/${it.key}", it.key) }

        // The harness and the row stylesheet shared by every fixture for consistency.
        staticResources("/harness", "harness")

        // Third-party runtimes like React.
        staticResources("/vendor", "vendor")

        // Control plane is served from the root.
        staticResources("/", "control")
    }
}
