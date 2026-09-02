/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.bench.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RungTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }

        val prettyJson by lazy {
            Json {
                prettyPrint = true
                encodeDefaults = true
            }
        }
    }

    inline fun <reified E : Exception> assertThrowsIf(shouldThrow: Boolean, block: () -> Unit) =
        if (shouldThrow) kotlin.test.assertFailsWith<E> { block() } else block()


    @Test
    fun `temporary rung-level tests`() {
        tLogger.info { "temporary rung tests - rungs is a hallucination to be refactored out" }

        listOf(
            Triple("Divisions", Rung.DIVISIONS, Level.DIVISIONS),
            Triple("Groups", Rung.GROUPS, Level.GROUPS),
            Triple("Teams", Rung.TEAMS, Level.TEAMS),
            Triple("People", Rung.PEOPLE, Level.PEOPLE),
            Triple("Reveal", Rung.REVEAL, null),
            Triple("Hallucinate", null, null)
        ).forEachIndexed { index, (key, rung, level) ->
            val rungByName = Rung.of(key)
            val levelByName = Level.entries.findLast { it.label == key }
            val levelByIndex by lazy { Level.at(index) }

            assertEquals(rung, rungByName, "rungByName matches")
            assertThrowsIf<IndexOutOfBoundsException>(index >= Rung.entries.size) {
                assertEquals(rung, Rung.entries[index], "rungByIndex matches")
            }

            assertEquals(level, levelByName, "levelByName matches")
            assertEquals(level, levelByIndex, "levelByIndex matches")

            when {
                index >= Level.entries.size -> assertEquals(null, levelByName, "Rung be bigger")
                else -> {
                    assertEquals(level, levelByIndex, "levelByIndex matches")
                    assertEquals(index, levelByName?.depth, "levelByName depth matches")
                    assertSame(levelByIndex, levelByName, "levelByName is level at depth")
                }
            }
        }
    }

    @Test
    fun `quick de-and-serializer test`() {
        mapOf(
            "Divisions" to Rung.DIVISIONS,
            "Groups" to Rung.GROUPS,
            "Teams" to Rung.TEAMS,
            "People" to Rung.PEOPLE,
            "Reveal" to Rung.REVEAL
        ).forEach { (key, rung) ->
            assertEquals(
                "\"${key.uppercase()}\"",
                prettyJson.encodeToString(rung),
                "JSON is just upper name"
            )

            assertEquals(
                rung,
                Json.decodeFromString<Rung>("\"${key.uppercase()}\""),
                "Like, twice"
            )
        }
    }
}
