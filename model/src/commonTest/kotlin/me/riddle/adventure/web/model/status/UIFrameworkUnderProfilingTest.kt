/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.UI_FRAMEWORK_KOBWEB
import me.riddle.adventure.web.model.UI_FRAMEWORK_PURE_TS
import me.riddle.adventure.web.model.UI_FRAMEWORK_REACT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UIFrameworkUnderProfilingTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }
    }

    @Test
    fun `should find framework fixture by key and place it after par`() =
        listOf(
            Triple(UI_FRAMEWORK_PURE_TS, UIFrameworkUnderProfiling.PURE_TS, "Pure TS"),
            Triple(UI_FRAMEWORK_REACT, UIFrameworkUnderProfiling.REACT, "React Core"),
            Triple(UI_FRAMEWORK_KOBWEB, UIFrameworkUnderProfiling.KOBWEB, "KobWeb Composer Core")
        ).forEachIndexed { index, (key, frameworkUnderTest, label) ->
            assertEquals(frameworkUnderTest, UIFrameworkUnderProfiling.of(key.first), "Key selects the fixture")
            assertEquals(key.second, frameworkUnderTest.label, "Label represents the column values")
            assertEquals(label, frameworkUnderTest.label, "Label is the column header")
            assertEquals(index + 1, frameworkUnderTest.column, "Par owns column zero")
        }

    @Test
    fun `should know nothing of unknown keys`() = listOf(null, "", "   ", "vanilla", "Pure JS").forEach {
        assertNull(UIFrameworkUnderProfiling.of(it).also { found -> tLogger.trace { "UIFrameworkUnderProfiling of '$it' is $found" } }, "No such fixture")
    }

    @Test
    fun `quick de-and-serializer test`() = UIFrameworkUnderProfiling.entries.forEach {
        assertEquals("\"${it.name}\"", Json.encodeToString(it), "JSON is just the enum name")
        assertEquals(it, Json.decodeFromString<UIFrameworkUnderProfiling>("\"${it.name}\""), "Same the other way")
    }
}
