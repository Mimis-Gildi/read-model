package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.service.HumanResourcesDataService
import me.riddle.adventure.web.service.data.status.Board

private val logger = KotlinLogging.logger {}

/** One instance for the whole application; it generates on first request and holds thereafter. */
private val humanResources = HumanResourcesDataService()

fun Application.configureRouting() {


    routing {
        webSocket("/ws") { // the control plane: push-only, one matrix per change
            Board.join(this)
            try {
                for (frame in incoming) { logger.debug { "Received frame: $frame" } }
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
            val level = call.parameters["level"]?.toIntOrNull()
            when {
                dataset == null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["dataset"]}")
                level == null || level !in 0..3 ->
                    call.respond(HttpStatusCode.NotFound, "No such level: ${call.parameters["level"]}")
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
