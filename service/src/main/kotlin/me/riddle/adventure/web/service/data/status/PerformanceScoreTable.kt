package me.riddle.adventure.web.service.data.status

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import me.riddle.adventure.web.service.data.bench.Dataset
import java.util.concurrent.ConcurrentHashMap

/**
 * The scoreboard: the current [Matrix] plus everyone attached to it.
 *
 * Currently, the fixture writes to `this`, and `this` writes to the control plane.
 *
 * Thus, two kinds of clients share the socket:
 *
 * A control plane connects, gets the current state, and thereafter only listens for updates.
 * (Subject to change.)
 *
 * A fixture connects and sends a [Report] per measured rung.
 *
 * Both are held in the same set because both must see the board change -- a fixture is welcome to watch its own numbers.
 */
object PerformanceScoreTable {

    private val logger = KotlinLogging.logger {}

    private val sessions: MutableSet<DefaultWebSocketServerSession> = ConcurrentHashMap.newKeySet()

    @Volatile
    var current: Matrix = PAR
        private set

    /**
     * Register a control plane and hand it "the state" immediately.
     *
     * Send Vocabulary and State because the page needs to display controls and test status view.
     * [VOCABULARY] is a constant and should not be sent over in [publish].
     * [current] is the mutable state of the scoring table.
     */
    suspend fun join(session: DefaultWebSocketServerSession) {
        logger.debug { session.call.request.origin.run { "WS call $remoteAddress:$remoteHost:$remotePort" } }
        sessions += session
        session.sendSerialized(VOCABULARY)
        session.sendSerialized(current)
    }

    fun leave(session: DefaultWebSocketServerSession) {
        sessions -= session
        logger.debug { session.call.request.origin.run { "WS END $remoteAddress:$remoteHost:$remotePort" } }
    }

    /**
     * Receive a single module, rung, and report combination and add it to the [current] matrix.
     */
    suspend fun record(report: Report) {
        val module = Module.of(report.module)
        val rung = Rung.of(report.rung)
        when {
            module == null -> logger.warn { "Report names no known module: ${report.module}" }
            rung == null -> logger.warn { "Report names no such rung: ${report.rung}" }
            Dataset.of(report.dataset) == null -> logger.warn { "Report names no known dataset: ${report.dataset}" }
            else -> publish(current.with(report.dataset, rung, module.column, report.cell))
        }
    }

    /**
     * Replace the state and tell everyone.
     *
     * A control plane can die mid-broadcast; that must not take the other consumers with it, so the `send` is guarded.
     * The dead session unregisters itself in its own handler's `finally`.
     */
    suspend fun publish(matrix: Matrix) {
        current = matrix
        logger.info { "Broadcasting Matrix to ${sessions.size} sessions." }
        sessions.forEach { runCatching { it.sendSerialized(matrix) } }
    }
}
