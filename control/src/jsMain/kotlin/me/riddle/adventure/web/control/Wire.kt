/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.control

import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.status.Frame

/** Unknown keys are ignored on purpose: the service may grow a field before this page learns to draw it. */
private val wire = Json { ignoreUnknownKeys = true }

/**
 * A frame off `/ws`. kotlinx.serialization dispatches on the discriminator it wrote itself; nothing here re-derives it.
 * @see [connect] add event listener 'message'.
 */
fun frameOf(text: String): Frame? = runCatching {
    wire.decodeFromString(Frame.serializer(), text)
}.onFailure { logger.error(it) { "Unreadable frame: $text" } }.getOrNull()
