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
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Level
import me.riddle.adventure.web.service.data.service.HumanResourcesDataService
import me.riddle.adventure.web.service.data.status.Board
import me.riddle.adventure.web.service.data.status.Report

private val logger = KotlinLogging.logger {}

/** One instance for the whole application; it generates on the first request to memoize thereafter. */
private val humanResources = HumanResourcesDataService()

fun Application.configureRouting() {


    routing {
        // One socket, two kinds of client: control planes that only listen, and fixtures that
        // send a Report per measured rung. An unreadable frame is logged and skipped rather than
        // thrown on -- a fixture with a typo in its payload must not be able to drop the channel
        // the run is being recorded over.
        webSocket("/ws") {
            Board.join(this)
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> frame.readText().let { text ->
                            runCatching { Json.decodeFromString<Report>(text) }
                                .onSuccess { Board.record(it) }
                                .onFailure { logger.warn(it) { "Unreadable frame: $text" } }
                        }

                        else -> logger.debug { "Ignored non-text frame: $frame" }
                    }
                }
            } finally {
                Board.leave(this)
            }
        }

        get("/data/{dataset}") { // the whole tree, one dataset per request
            when (val dataset = Dataset.of(call.parameters["dataset"])) {
                null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["dataset"]}")
                else -> call.respond(humanResources.get(dataset))
            }
        }

        get("/data/{dataset}/{level}") { // the same tree culled to a level
            val dataset = Dataset.of(call.parameters["dataset"])
            val level = call.parameters["level"]?.toIntOrNull()?.let(Level::at)
            when {
                dataset == null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["dataset"]}")
                level == null -> call.respond(HttpStatusCode.NotFound, "No such level: ${call.parameters["level"]}")
                else -> call.respond(humanResources.get(dataset, level))
            }
        }

        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        staticResources("/static", "static")
        staticResources("/", "control")
    }
}
