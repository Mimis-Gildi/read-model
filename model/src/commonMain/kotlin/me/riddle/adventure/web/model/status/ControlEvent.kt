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
@SerialName("controlEvent")
data class ControlEvent(
    val runId: String,
    val fixture: String,
    val action: String,
) : Incoming
