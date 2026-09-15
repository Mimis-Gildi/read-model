/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
@file:JsExport

package me.riddle.adventure.web.model.bench

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.model.DATASET_LEVEL_0
import me.riddle.adventure.web.model.DATASET_LEVEL_1
import me.riddle.adventure.web.model.DATASET_LEVEL_2
import me.riddle.adventure.web.model.DATASET_LEVEL_3
import kotlin.js.JsExport

/**
 * [depth] names the deepest type a response still carries:
 * [TEAMS] is divisions, groups, and teams, with no people under them.
 */
@Serializable
enum class Level(val label: String) {
    DIVISIONS(DATASET_LEVEL_0),
    GROUPS(DATASET_LEVEL_1),
    TEAMS(DATASET_LEVEL_2),
    PEOPLE(DATASET_LEVEL_3);

    val depth: Int get() = ordinal

    companion object {

        fun at(depth: Int) = entries.getOrNull(depth)
    }
}
