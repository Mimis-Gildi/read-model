package me.riddle.adventure.web.service.data.status

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.websocket.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * The scoreboard: the current [Matrix] plus everyone attached to it.
 *
 * Currently, the fixture writes to `this`, and `this` writes to the control plane.
 *
 * Thus, two kinds of clients share the socket:
 *
 * A control plane connects, gets the current state, and thereafter only listens.
 * (Subject to change.)
 *
 * A fixture connects and sends a [Report] per measured rung.
 *
 * Both are held in the same set because both must see the board change -- a fixture is welcome to watch its own numbers.
 */
object Board {

    private val sessions: MutableSet<DefaultWebSocketServerSession> = ConcurrentHashMap.newKeySet()

    @Volatile
    var current: Matrix = PAR
        private set

    /**
     * Register a control plane and hand it "the state" immediately.
     *
     * First the Vocabulary, then the State: the page needs to know what may be asked for before it is shown.
     * [VOCABULARY] is a constant, so it has no business in [publish]; [current] stays the only mutable on the board.
     */
    suspend fun join(session: DefaultWebSocketServerSession) {
        sessions += session
        session.sendSerialized(VOCABULARY)
        session.sendSerialized(current)
    }

    fun leave(session: DefaultWebSocketServerSession) {
        sessions -= session
    }

    /**
     * Fold a fixture's measurement into the board and broadcast the result.
     *
     * Unaddressable reports are dropped: the socket is also the debugging channel, and a fixture naming a module or
     * a level the matrix has no cell for must not be able to close it.
     *
     * By @rdd13r's design (yours truly): the board holds ONE and only one matrix for the whole service. Consequently,
     * [Report.run] and [Report.dataset] are carried for debugging and posterity reasons, but not directly used for any
     * other purpose. This means two runs against different datasets overwrite each other's cells. It is possible and
     * easy to make the table react to a selected dataset yet seemed not worth the 2-way communication addition.
     * Keying the board by run is a separate change altogether.
     */
    suspend fun record(report: Report) {
        val module = Module.of(report.module)
        when {
            module == null -> logger.warn { "Report names no known module: ${report.module}" }
            report.level !in current.rows.indices -> logger.warn { "Report names no such level: ${report.level}" }
            else -> publish(current.with(report.level, module.column, report.cell))
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
        sessions.forEach { runCatching { it.sendSerialized(matrix) } }
    }
}
