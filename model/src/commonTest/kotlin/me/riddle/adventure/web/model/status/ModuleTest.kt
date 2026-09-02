/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModuleTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }
    }

    @Test
    fun `should find module by key and place it after par`() =
        listOf(
            Triple("pure", Module.VANILLA, "Pure JS"),
            Triple("react-core", Module.REACT_CORE, "React Core"),
            Triple("react-full", Module.REACT_FULL, "React Full"),
            Triple("kobweb", Module.KOBWEB, "Kobweb (Full)"),
        ).forEachIndexed { index, (key, module, label) ->
            assertEquals(module, Module.of(key), "Key selects the fixture")
            assertEquals(label, module.label, "Label is the column header")
            assertEquals(index + 1, module.column, "Par owns column zero")
        }

    @Test
    fun `should know nothing of unknown keys`() = listOf(null, "", "   ", "vanilla", "Pure JS").forEach {
        assertNull(Module.of(it).also { found -> tLogger.trace { "Module of '$it' is $found" } }, "No such fixture")
    }

    @Test
    fun `quick de-and-serializer test`() = Module.entries.forEach {
        assertEquals("\"${it.name}\"", Json.encodeToString(it), "JSON is just the enum name")
        assertEquals(it, Json.decodeFromString<Module>("\"${it.name}\""), "Same the other way")
    }
}
