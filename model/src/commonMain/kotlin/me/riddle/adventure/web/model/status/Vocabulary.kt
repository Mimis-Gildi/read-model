/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

/** ToDo: For now, key is the mount point too; may change for kobweb. */
@Serializable
data class ModuleOption(val key: String, val label: String)

@Serializable
@SerialName("vocabulary")
data class Vocabulary(
    val modules: List<ModuleOption>,
    val datasets: List<DatasetOption>,
) : Frame
