package me.riddle.adventure.web.service.data.status

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import java.util.Collections
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

    /** Register a control plane and hand it the state immediately -- not on the next change. */
    suspend fun join(session: DefaultWebSocketServerSession) {
        sessions += session
        session.sendSerialized(current)
    }

    fun leave(session: DefaultWebSocketServerSession) {
        sessions -= session
    }

    /**
     * Replace the state and tell everyone. No caller yet -- the fixture endpoints will call it.
     *
     * A control plane can die mid-broadcast; that must not take the others with it, so the send
     * is guarded. The dead session unregisters itself in its own handler's `finally`.
     */
    suspend fun publish(matrix: Matrix) {
        current = matrix
        sessions.forEach { runCatching { it.sendSerialized(matrix) } }
    }
}
