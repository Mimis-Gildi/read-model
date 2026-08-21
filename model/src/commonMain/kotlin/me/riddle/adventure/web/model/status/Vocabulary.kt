/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

@Serializable
data class FrameworkOption(val key: String, val label: String)

@Serializable
@SerialName("vocabulary")
data class Vocabulary(
    val frameworks: List<FrameworkOption>,
    val datasets: List<DatasetOption>,
) : Frame
