package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Level

@Serializable
enum class Rung(val label: String) {
    DIVISIONS(Level.DIVISIONS.label),
    GROUPS(Level.GROUPS.label),
    TEAMS(Level.TEAMS.label),
    PEOPLE(Level.PEOPLE.label),

    REVEAL("Reveal");

    companion object {

        fun of(label: String?) = entries.find { it.label == label }

    }
}
