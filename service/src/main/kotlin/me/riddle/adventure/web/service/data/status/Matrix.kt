package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * The control plane renders a table with comparative testrun results.
 * All the statistical information is pushed on every change: headers, row titles, cells.
 * The control plane page replaces its view fully having no business in business logic.
 * Control plane holds no model of its own and must faithfully present whatever it's given.
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
 * The same matrix with one cell replaced -- how a [Report] reflects on the board after its creation.
 *
 * A copy rather than a mutation: [Board.current] is read by every broadcast and by every joining control plane,
 * so it is swapped whole rather than edited underneath them.
 *
 * An out-of-range address changes nothing. A fixture reporting a level or module the matrix has no cell for is a bug
 * in the fixture, and dropping the value is better than either throwing on the socket or growing the matrix to fit
 * whatever arrived.
 */
fun Matrix.with(row: Int, column: Int, value: String): Matrix = copy(
    rows = rows.mapIndexed { index, matrixRow ->
        when (index) {
            row -> matrixRow.copy(
                cells = matrixRow.cells.mapIndexed { at, cell -> when (at) { column -> value; else -> cell } },
            )

            else -> matrixRow
        }
    },
)

/**
 * In the real world these are the C++ numbers of Google's own implementation on Riddler's machine.
 * (If you have a way different machine, then knock yourself out and change them here.)
 *
 * One row per level of the read model -- divisions, groups, teams, people -- the demoscene type names rather than the
 * index levels of nesting. So a row title says what was rendered without counting the four layers.
 *
 */
val PAR: Matrix = Matrix(
    // Par, then one column per module -- composed off the enum so a column and the [Module.column]
    // a report is folded into cannot disagree about which framework owns which cell.
    columns = listOf("Par") + Module.entries.map { it.label },
    rows = listOf(
        MatrixRow("Divisions", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("Groups", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("Teams", listOf("0.0", BLANK, BLANK, BLANK)),
        MatrixRow("People", listOf("0.0", BLANK, BLANK, BLANK)),
    ),
)
