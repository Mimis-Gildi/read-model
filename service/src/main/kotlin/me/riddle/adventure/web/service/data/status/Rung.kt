package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Level

/**
 * The row axis of the [Matrix]: everything a fixture can measure and put a number against.
 *
 * Deliberately not [Level]. A level is a depth on the URL -- what the service culled the tree to -- and there are
 * exactly four of those. A rung is a *measurement*, and the reveal is one that has no depth: it is the People already
 * on the page finally being laid out, chunk by chunk, until the browser gives up. Folding it into [Level] would have
 * made `/data/bench/4` a legal URL; folding it into [Level.PEOPLE] would have overwritten the ladder's People cell,
 * which measures something else entirely -- building 187,500 folded rows from nothing.
 *
 * The four level labels are taken from [Level] rather than retyped, so the row title and the culled tree cannot come
 * to disagree about what a row is called. The constant names are the one thing said twice; Kotlin has no way to grow
 * an enum from another one, and a stringly-typed row key is the worst tradeoff I can think of.
 *
 * This is necessary because the Chrome browser cannot expand over 100k nodes in one go.
 */
@Serializable
enum class Rung(val label: String) {
    DIVISIONS(Level.DIVISIONS.label),
    GROUPS(Level.GROUPS.label),
    TEAMS(Level.TEAMS.label),
    PEOPLE(Level.PEOPLE.label),

    /** Unfolding what the ladder built folded. Reported per chunk, accumulating, until it finishes or dies. */
    REVEAL("Reveal");

    companion object {

        /** Resolves the label a fixture puts on the wire. An unknown one resolves to nothing and is dropped. */
        fun of(label: String?) = entries.find { it.label == label }
    }
}
