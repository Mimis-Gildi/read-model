/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.model.bench.Level
import kotlin.js.JsExport

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
