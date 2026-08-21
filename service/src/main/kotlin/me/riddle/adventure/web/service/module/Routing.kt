/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.*
import me.riddle.adventure.web.model.bench.Dataset
import me.riddle.adventure.web.model.bench.Level
import me.riddle.adventure.web.model.status.Incoming
import me.riddle.adventure.web.model.status.UIFrameworkUnderProfiling
import me.riddle.adventure.web.service.data.service.HumanResourcesDataService
import me.riddle.adventure.web.service.data.status.PerformanceScoreTable
import me.riddle.adventure.web.model.status.UIFrameworkUnderProfiling.entries as allFixtures

private val logger = KotlinLogging.logger {}

/** One instance for the whole application; it generates on the first request to memoize thereafter. */
private val humanResources = HumanResourcesDataService()

/**
 * The `/data` contract as types. Segment names bind to constructor properties by name, so the template and the
 * handler can no longer drift apart, and [io.ktor.server.resources.href] builds these URLs in the other direction.
 */
@Serializable
@Resource(DATA_RESOURCE_SLUG)
class DataResource {

    @Serializable
    @Resource("{$PARAMETER_DATASET}")
    class OfDataset(@Suppress("unused") val parent: DataResource = DataResource(), val datasetKey: String) {

        @Serializable
        @Resource("{$PARAMETER_LEVEL}")
        class AtLevel(val parent: OfDataset, val level: String)
    }
}

fun Application.configureRouting() {

    routing {

        webSocket(EVENT_CHANNEL) {
            PerformanceScoreTable.join(this)
            try {
                incoming
                    .consumeAsFlow()
                    .filterIsInstance<Frame.Text>()
                    .map { it.readText() }
                    .onEach { text -> logger.debug { "Received frame: $text" } }
                    .mapNotNull { text ->
                        runCatching { Json.decodeFromString<Incoming>(text) }
                            .onFailure { error -> logger.warn(error) { "Unreadable frame: $text" } }
                            .getOrNull()
                    }
                    .collect { message ->
                        PerformanceScoreTable.record(message)
                        logger.debug { "Recorded $message" }
                    }
            } finally {
                PerformanceScoreTable.leave(this)
            }
        }

        get<DataResource.OfDataset> { route ->
            when (val dataset = Dataset.of(route.datasetKey)) {
                null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${route.datasetKey}")
                else -> call.respond(humanResources.get(dataset))
            }
        }

        get<DataResource.OfDataset.AtLevel> { route ->
            val dataset = Dataset.of(route.parent.datasetKey)
            val level = route.level.toIntOrNull()?.let(Level::at)
            when {
                dataset == null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${route.parent.datasetKey} with level: $level.")
                level == null -> call.respond(HttpStatusCode.NotFound, "No such level: ${route.level} with dataset: $dataset.")
                else -> call.respond(humanResources.get(dataset, level))
            }
        }

        (allFixtures.map(UIFrameworkUnderProfiling::key)
                + UI_COMPONENT_HARNESS
                + UI_COMPONENT_VENDOR
                ).forEach { component ->
                staticResources("/$component", component)
            }

        staticResources("/", UI_COMPONENT_CONTROL)
    }
}
