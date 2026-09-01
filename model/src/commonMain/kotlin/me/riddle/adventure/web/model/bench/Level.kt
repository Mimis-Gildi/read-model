/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: $REFACTORED%.
 * The remainder is validated prototyping slop,
 *   provisionally accepted and temporary.
 */

package me.riddle.adventure.web.model.bench

import kotlinx.serialization.Serializable

/**
 * [depth] names the deepest type a response still carries:
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

        fun at(depth: Int) = entries.getOrNull(depth)
    }
}
