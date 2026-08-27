package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Level

/**
 * The control plane renders a table with comparative testrun results.
 * All the statistical information is pushed on every change: headers, row titles, cells.
 * The control plane page replaces its view fully having no business in business logic.
 * It holds no model of its own and must faithfully present whatever it's given -- with one exception it is allowed:
 * it keeps the last frame so it can re-show it for whichever dataset the operator picks. Choosing is not modelling.
 *
 * [MatrixRow.dataset] is the key that makes that possible. It is never a column and is never rendered.
 *
 * Cells are pre-formatted strings on purpose: rounding, units, and the status vocabulary
 * ("crashed", [BLANK], ...) live here in one place instead of being duplicated in JavaScript.
 */
@Serializable
data class MatrixRow(val dataset: String, val title: String, val cells: List<String>)

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
 * A cell is addressed by *(dataset, level, module)*, never by position. Rows are keyed rather than counted because
 * every number this system produces belongs to one dataset, and a `matrix` that only knew the level let a LOAD run
 * quietly overwrite a BENCH -- the one and only one: in the same cell.
 *
 * A copy rather than a mutation: [Board.current] is read by every broadcast and by every joining control plane,
 * so it is swapped whole rather than edited underneath them.
 *
 * An address that matches nothing does not change anything. A fixture reporting a dataset, level, or module the matrix
 * has no cell for is a bug in the fixture. Dropping the value is better than either throwing on the socket or growing
 * the matrix to fit whatever garbage arrived.
 */
fun Matrix.with(dataset: String, level: Level, column: Int, value: String): Matrix = copy(
    rows = rows.map { row ->
        when {
            row.dataset != dataset || row.title != level.label -> row
            else -> row.copy(cells = row.cells.mapIndexed { at, cell -> when (at) { column -> value; else -> cell } })
        }
    },
)

/**
 * In the real world these are the C++ numbers of Google's own implementation on Riddler's machine.
 * (If you have a way different machine, then knock yourself out and change them here.)
 *
 * One row per dataset per level -- twelve of them -- because par is a property of the tree that was rendered, and the
 * three trees are of wildly different sizes. Rows are named for the demoscene type rather than the index of nesting,
 * so a row title says what was rendered without counting the four layers.
 *
 * PLACEHOLDER VALUES -- zeros, so nobody mistakes them for a measurement. @rdd13r sets the real ones.
 *
 * FixMe: Add Chrome baseline after initial PAR.
 */
val PAR: Matrix = Matrix(
    // Par, then one column per module -- composed off the enum so a column and the [Module.column]
    // a report is folded into cannot disagree about which framework owns which cell.
    columns = listOf("Par") + Module.entries.map { it.label },
    rows = Dataset.entries.flatMap { dataset ->
        Level.entries.map { level -> MatrixRow(dataset.key, level.label, listOf("0.0") + Module.entries.map { BLANK }) }
    },
)
