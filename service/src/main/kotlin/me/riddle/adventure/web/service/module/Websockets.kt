package me.riddle.adventure.web.service.module

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebsockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(
            // `type` is a defaulted discriminator or kotlinx drops it.
            Json { encodeDefaults = true }
        )
    }
}
