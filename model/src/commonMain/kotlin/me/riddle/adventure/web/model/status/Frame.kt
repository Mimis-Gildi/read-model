/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/** One `Event Channel` [me.riddle.adventure.web.model.EVENT_CHANNEL] message. kotlinx.serialization writes and reads the discriminator; no hand-typed `type` field needed. */
@Serializable
sealed interface Frame
