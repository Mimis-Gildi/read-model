package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import me.riddle.adventure.web.service.data.status.Board

private val logger = KotlinLogging.logger {}

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

        get("/data/{dataset}/{level}") {

        }

        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        staticResources("/static", "static")
        staticResources("/", "control")
    }
}
