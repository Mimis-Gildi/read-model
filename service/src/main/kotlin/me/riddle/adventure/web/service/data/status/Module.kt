package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * The frameworks under test -- one per column of the [Matrix], in the order they are compared.
 *
 * [key] is what the fixture carries on its URL and puts on the wire; [label] is what the column header reads.
 * They differ, so the label is stored rather than derived.
 */
@Serializable
enum class Module(val key: String, val label: String) {
    VANILLA("pure", "Pure JS"),
    REACT_CORE("react-core", "React Core"),
    REACT_FULL("react-full", "React Full"),
    KOBWEB("kobweb", "Kobweb");

    /** The cell this module owns in a row. Par is the column zero, so the modules start after it. */
    val column: Int get() = ordinal + 1

    companion object {

        fun of(key: String?) = entries.find { it.key == key }
    }
}
