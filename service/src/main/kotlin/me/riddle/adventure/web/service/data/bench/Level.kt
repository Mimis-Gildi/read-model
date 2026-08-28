package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

/**
 * [depth] travels on the URL and names the deepest type a response still carries:
 * [TEAMS] is divisions, groups, and teams, with no people under them.
 */
@Serializable
enum class Level(val label: String) {
    DIVISIONS("Divisions"),
    GROUPS("Groups"),
    TEAMS("Teams"),
    PEOPLE("People");

    val depth: Int get() = ordinal

    companion object {

        /** Null for an unknown depth; 404 rather than default. */
        fun at(depth: Int) = entries.getOrNull(depth)
    }
}
