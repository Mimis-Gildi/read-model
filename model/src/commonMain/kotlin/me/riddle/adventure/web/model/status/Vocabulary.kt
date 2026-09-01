/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable

@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

/** ToDo: For now, key is the mount point too; may change for kobweb. */
@Serializable
data class ModuleOption(val key: String, val label: String)

@Serializable
data class Vocabulary(
    val type: String = "vocabulary",
    val modules: List<ModuleOption>,
    val datasets: List<DatasetOption>,
)
