package me.riddle.adventure.web.service.data.status

import io.ktor.server.websocket.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * The scoreboard: the current [Matrix] plus every control plane watching it.
 *
 * Push-only. Control planes never send; they connect, get the current state,
 * and then receive whatever [publish] hands out.
 */
object Board {

    // ToDo: Find a better way to present the mutable set.
    private val sessions: MutableSet<DefaultWebSocketServerSession> = Collections.newSetFromMap(ConcurrentHashMap())

    @Volatile
    var current: Matrix = PAR
        private set

    /**
     * Register a control plane and hand it the state immediately instead of the next change.
     *
     * Vocabulary first, then state: the page needs to know what may be asked for before it is shown what was measured.
     * [VOCABULARY] is a constant, so it has no business in [publish]; [current] stays the only mutable thing on the board.
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
     * Replace the state and tell everyone. No callers and the fixture endpoints will call it eventually.
     *
     * A control plane can die mid-broadcast; that must not take the others with it, so the `send` is guarded.
     * The dead session unregisters itself in its own handler's `finally`.
     */
    suspend fun publish(matrix: Matrix) {
        current = matrix
        sessions.forEach { runCatching { it.sendSerialized(matrix) } }
    }
}
