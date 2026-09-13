/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.control

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.Vocabulary

/**
 * A frame off `/ws`. The service names its own kind in `type`.
 * ToDo: remap /ws
 * ToDo: URL off of field not key on vocab.
 */
sealed interface Frame {

    data class Scores(val performanceComparisonTable: PerformanceComparisonTable) : Frame

    data class Words(val vocabulary: Vocabulary) : Frame
}

/** Unknown keys are ignored on purpose: the service may grow a field before this page learns to draw it. */
private val wire = Json { ignoreUnknownKeys = true }

/**
 * Unexpected frames will not draw.
 * @see [connect] add event listener 'message'. */
fun frameOf(text: String): Frame? = runCatching {
    wire.parseToJsonElement(text).jsonObject.let { frame ->
        when (frame["type"]?.jsonPrimitive?.content) {
            "performanceComparisonTable" -> Frame.Scores(wire.decodeFromJsonElement(PerformanceComparisonTable.serializer(), frame))
            "vocabulary" -> Frame.Words(wire.decodeFromJsonElement(Vocabulary.serializer(), frame))
            else -> null.also { logger.warn { "Unknown frame type: ${frame["type"]}" } }
        }
    }
}.getOrNull()
