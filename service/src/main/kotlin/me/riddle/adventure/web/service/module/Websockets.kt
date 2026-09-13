/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */
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
        // `type` is now the polymorphic discriminator kotlinx.serialization writes for `Frame`; no config needed for it.
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}
