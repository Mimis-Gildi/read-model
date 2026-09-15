/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.window
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

fun connect() {
    setConnectionStatusControl("wait", "connecting")

    val proto = if (window.location.protocol == "https:") "wss:" else "ws:"
    val socket = runCatching { WebSocket("$proto//${window.location.host}/ws") }.getOrElse {
        setConnectionStatusControl("stale", "offline")
        logger.error { "Control socket refused to open: ${it.message}" }
        return
    }

    socket.addEventListener("open", { setConnectionStatusControl("live", "live") })

    socket.addEventListener("message", { event ->
        when (val frame = frameOf((event as MessageEvent).data.toString())) {
            is Vocabulary -> {
                renderVocabulary(frame)
                render(performanceComparisonTable)
            }

            is PerformanceComparisonTable -> {
                performanceComparisonTable = frame
                render(performanceComparisonTable)
            }

            null -> logger.warn { "Received unknown frame!" }
        }
    })

    socket.addEventListener("close", {
        setConnectionStatusControl("stale", "offline")
        logger.error { "Control socket closed. Reload once the service is back." }
    })

    socket.addEventListener("error", { runCatching { socket.close() } })
}
