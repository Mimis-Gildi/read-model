package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * The frameworks under test -- one per column of the [Matrix], in the order they are compared.
 *
 * The enum exists because a [Report] arrives naming its module as a string and something has to turn that into a column.
 * Hard-coding `"vanilla" -> 1` in the fold would be a second copy of the column order, and the copies drift -- which is
 * exactly how [PAR] came to be missing Kobweb.
 *
 * [key] is what the fixture carries on its URL and puts on the wire;
 * [label] is what the column header reads.
 * They differ, so unlike [me.riddle.adventure.web.service.data.bench.Dataset] the label cannot be derived and is stored.
 */
@Serializable
enum class Module(val key: String, val label: String) {
    VANILLA("vanilla", "Vanilla JS"),
    REACT("react", "React"),
    KOBWEB("kobweb", "Kobweb");

    /** The cell this module owns in a row. Par is a column zero, so the modules start after it. */
    val column: Int get() = ordinal + 1

    companion object {

        /** Resolves the [key] carried on the URL and in a [Report], so the wire never sees the constant name. */
        fun of(key: String?) = entries.find { it.key == key }
    }
}
