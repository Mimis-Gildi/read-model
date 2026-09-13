/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not started as prototyping slop, but needs a field.
 *
 * ToDo: consider pointing at fixture instead of using a key.
 */

@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.model.UI_FRAMEWORK_KOBWEB
import me.riddle.adventure.web.model.UI_FRAMEWORK_KOBWEB_APP
import me.riddle.adventure.web.model.UI_FRAMEWORK_PURE_TS
import me.riddle.adventure.web.model.UI_FRAMEWORK_REACT
import me.riddle.adventure.web.model.UI_FRAMEWORK_REACT_APP
import kotlin.js.JsExport

/**
 * The fixture is currently selected by the [key] sub-URL.
 * The [label] is the column header selector.
 */
@Serializable
enum class UIFrameworkUnderProfiling(val key: String, val label: String) {
    PURE_TS(UI_FRAMEWORK_PURE_TS.first, UI_FRAMEWORK_PURE_TS.second),
    REACT(UI_FRAMEWORK_REACT.first, UI_FRAMEWORK_REACT.second),
    REACT_APP(UI_FRAMEWORK_REACT_APP.first, UI_FRAMEWORK_REACT_APP.second),
    KOBWEB(UI_FRAMEWORK_KOBWEB.first, UI_FRAMEWORK_KOBWEB.second),
    KOBWEB_APP(UI_FRAMEWORK_KOBWEB_APP.first, UI_FRAMEWORK_KOBWEB_APP.second);

    /** Par owns column zero, so the modules start after it. */
    val column: Int get() = ordinal + 1

    companion object {

        fun of(key: String?) = entries.find { it.key == key }
    }
}
