/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.window
import me.riddle.adventure.web.model.ConnectionStatus.CONNECTING
import me.riddle.adventure.web.model.ConnectionStatus.LIVE
import me.riddle.adventure.web.model.ConnectionStatus.OFFLINE
import me.riddle.adventure.web.model.ON_CLOSE
import me.riddle.adventure.web.model.ON_ERROR
import me.riddle.adventure.web.model.ON_MESSAGE
import me.riddle.adventure.web.model.ON_OPEN
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

/** Whatever served the control plane serves `/ws`; there is no second host and no TLS variant of this page. */
private fun controlPlane() = "ws://${window.location.host}/ws"

/**
 * Opened once, like the fixtures do. A dead socket means a dead service, and a page that quietly reconnects behind a
 * stale table is worse than one that says it is offline: reload once the service is back.
 */
fun connect() = setConnectionStatusControl(CONNECTING)
    .let { runCatching { WebSocket(controlPlane()) } }
    .onFailure { offline("Control socket refused to open: ${it.message}") }
    .onSuccess { socket ->
        listOf<Pair<String, (Event) -> Unit>>(
            ON_OPEN to { setConnectionStatusControl(LIVE) },
            ON_MESSAGE to { event -> receive(event) },
            ON_CLOSE to { offline("Control socket closed. Reload once the service is back.") },
            ON_ERROR to { runCatching { socket.close() } },
        ).fold(socket) { open, (event, handler) -> open.apply { addEventListener(event, handler) } }
    }

private fun offline(why: String) = setConnectionStatusControl(OFFLINE).also { logger.error { why } }

/** The frame's own discriminator says which of the two it is; the control plane renders whatever arrived. */
private fun receive(event: Event) = when (val frame = frameOf((event as MessageEvent).data.toString())) {
    is Vocabulary -> renderVocabulary(frame).also { render(performanceComparisonTable) }
    is PerformanceComparisonTable -> render(frame.also { performanceComparisonTable = it })
    null -> logger.warn { "Received unknown frame!" }
}
