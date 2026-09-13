/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Refactored: 70%.
 * The remainder is validated prototyping slop,
 *   provisionally accepted and temporary.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.window
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import kotlin.math.min
import kotlin.math.pow

private var attempt = 0
private var retryTimer: Int? = null

fun connect() {
    setConnectionStatusControl("wait", if (attempt == 0) "connecting" else "reconnecting")

    val proto = if (window.location.protocol == "https:") "wss:" else "ws:"
    val socket = runCatching { WebSocket("$proto//${window.location.host}/ws") }.getOrElse {
        setConnectionStatusControl("stale", "offline")
        scheduleRetry()
        return
    }

    socket.addEventListener("open", {
        attempt = 0
        setConnectionStatusControl("live", "live")
    })

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
        scheduleRetry()
    })

    // The one place that schedules the retry.
    socket.addEventListener("error", { runCatching { socket.close() } })
}

/** Backs off to a five-second ceiling. ToDo: refactor out. */
private fun scheduleRetry() {
    if (retryTimer != null) return

    val delay = min(100.0 * 2.0.pow(attempt), 5000.0).toInt()
    attempt += 1
    retryTimer = window.setTimeout({
        retryTimer = null
        connect()
    }, delay)
}
