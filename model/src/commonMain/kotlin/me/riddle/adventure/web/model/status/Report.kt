/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/** One `Event Channel` [me.riddle.adventure.web.model.EVENT_CHANNEL] message read from a client. kotlinx.serialization writes and reads the discriminator; no hand-typed `type` field needed. */
@Serializable
sealed interface Incoming

@Serializable
@SerialName("report")
data class Report(
    val runId: String,
    val uiFramework: String,
    val datasetKey: String,
    val rung: String,
    val elements: Int,
    val built: Double,
    val painted: Double,
) : Incoming
