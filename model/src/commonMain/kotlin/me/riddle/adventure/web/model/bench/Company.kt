/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.bench

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.model.DATASET_KEY_BENCH
import me.riddle.adventure.web.model.DATASET_KEY_CEILING
import me.riddle.adventure.web.model.DATASET_KEY_DEEP
import me.riddle.adventure.web.model.DATASET_KEY_EDGE
import me.riddle.adventure.web.model.DATASET_KEY_FAIL
import me.riddle.adventure.web.model.DATASET_KEY_LOAD
import me.riddle.adventure.web.model.DATASET_KEY_SHALLOW
import me.riddle.adventure.web.model.DATASET_KEY_SMOKE
import me.riddle.adventure.web.model.DATASET_KEY_STRESS
import kotlin.js.JsExport

@Serializable
data class Company(
    val dataset: Dataset,
    val divisions: List<CorporateDivision>
)

@Serializable
enum class Dataset(
    val key: String,
    val branchCountGroups: Int,
    val branchCountTeams: Int,
    val branchCountPeople: Int
) {
    /** The most common production counts: 1.5k nodes. */
    SMOKE(DATASET_KEY_SMOKE, 7, 3, 4),
    /** Typical large application counts: under 20k nodes. */
    BENCH(DATASET_KEY_BENCH, 9, 11, 13),
    /** Largest practical presentation: 50k nodes. */
    STRESS(DATASET_KEY_STRESS, 13, 15, 19),
    /** Balanced branching: ~100k nodes. */
    CEILING(DATASET_KEY_CEILING, 16, 19, 25),
    /** [CEILING] count over many tiny teams: ~100k nodes. */
    SHALLOW(DATASET_KEY_SHALLOW, 20, 128, 2),
    /** [CEILING] count over a few large teams: ~100k nodes. */
    DEEP(DATASET_KEY_DEEP, 2, 2, 1921),
    /** Conferences accepted ceiling: ~200k nodes. */
    EDGE(DATASET_KEY_EDGE, 19, 25, 35),
    /** Maximum profiled browser load: ~300k nodes. */
    LOAD(DATASET_KEY_LOAD, 20, 30, 40),
    /** Ludicrous 1M node load. */
    FAIL(DATASET_KEY_FAIL, 32, 45, 55);

    val nodes: Int
        get() = CorporateDivision.DIVISIONS.size *
                (1 + branchCountGroups + branchCountGroups * branchCountTeams + branchCountGroups * branchCountTeams * branchCountPeople)

    companion object {

        /** Resolves the [key] carried on the URL, so the wire never sees the constant name. */
        fun of(key: String?) = entries.find { it.key == key }
    }
}
