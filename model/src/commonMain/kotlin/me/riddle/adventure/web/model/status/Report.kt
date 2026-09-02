/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no prototyping slop remaining.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
data class Report(
    val type: String = "report",
    val run: String,
    val module: String,
    val dataset: String,
    val rung: String,
    val elements: Int,
    val built: Double,
    val painted: Double,
)
