package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * What the control plane renders. The whole thing is pushed on every change
 *   -- headers, row titles, cells -- and the page replaces its view wholesale.
 *   It holds no model of its own.
 *
 * Cells are pre-formatted strings on purpose: rounding, units, and the status vocabulary
 * ("crashed", [BLANK], ...) live here in one place instead of being duplicated in JavaScript.
 */
@Serializable
data class MatrixRow(val title: String, val cells: List<String>)

@Serializable
data class Matrix(
    val type: String = "matrix",
    val columns: List<String>,
    val rows: List<MatrixRow>,
)

/** Dash. No value yet. */
const val BLANK: String = " - "

/**
 * Par is the Riddler's claim about what these numbers should be in the real world.
 *
 * One row per level of the read model -- divisions, groups, teams, people -- named after the type
 * rather than the index, so a row title says what was rendered without anyone counting.
 *
 * PLACEHOLDER VALUES -- zeros, so nobody mistakes them for a measurement. @rdd13r sets the real ones.
 */
val PAR: Matrix = Matrix(
    columns = listOf("Par", "Vanilla JS", "React", "Kobweb"),
    rows = listOf(
        MatrixRow("Divisions", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("Groups", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("Teams", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("People", listOf("0.0", BLANK, BLANK, BLANK)),
    ),
)
