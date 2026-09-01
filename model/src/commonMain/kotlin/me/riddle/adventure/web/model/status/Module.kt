/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not started as prototyping slop, but needs a field.
 *
 * ToDo: consider pointing at fixture instead of using a key.
 */

@file:JsExport
@file:OptIn(ExperimentalJsExport::class)

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * The fixture is currently selected by the [key] sub-URL.
 * The [label] is the column header selector.
 */
@Serializable
enum class Module(val key: String, val label: String) {
    VANILLA("pure", "Pure JS"),
    REACT_CORE("react-core", "React Core"),
    REACT_FULL("react-full", "React Full"),
    KOBWEB("kobweb", "Kobweb (Full)");

    /** Par owns column zero, so the modules start after it. */
    val column: Int get() = ordinal + 1

    companion object {

        fun of(key: String?) = entries.find { it.key == key }
    }
}
