package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset

/**
 * The control plane renders a table with comparative testrun results.
 * All the statistical information is pushed on every change: headers, row titles, cells.
 * The control plane page replaces its view fully having no business in business logic.
 * It holds no model of its own and must faithfully present whatever it's given -- with one exception it is allowed:
 * it keeps the last frame so it can re-show it for whichever dataset the operator picks. Choosing != modeling.
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
 * A cell should be addressed by *(dataset, rung, module)*, not by position. Rows are keyed rather than counted because
 * every number this system produces belongs to one dataset, and a `matrix` that only knew the level let a LOAD run
 * quietly overwrite a BENCH -- the one and only one: in the same cell.
 *
 * A copy rather than a mutation: [Board.current] is read by every broadcast and by every joining control plane,
 * so it is swapped whole rather than edited underneath them.
 *
 * An address that matches nothing does not change anything. A fixture reporting a dataset, rung, or module the matrix
 * has no cell for is a bug in the fixture. Dropping the value is better than either throwing on the socket or growing
 * the matrix to fit whatever garbage arrived.
 */
fun Matrix.with(dataset: String, rung: Rung, column: Int, value: String): Matrix = copy(
    rows = rows.map { row ->
        when {
            row.dataset != dataset || row.title != rung.label -> row
            else -> row.copy(cells = row.cells.mapIndexed { at, cell -> when (at) { column -> value; else -> cell } })
        }
    },
)

/**
 * Chrome's own JSON viewer rendering the same tree on Riddler's machine, in milliseconds, read off a recorded
 * performance profile. (If you have a way different machine, then knock yourself out and change them here.)
 *
 * Not a C++ number, and worth saying plainly because the opposite is the flattering assumption: the DevTools JSON
 * viewer is a web app on the same DOM APIs this benchmark uses. V8's parse is native, but parsing sits outside the
 * clock on both sides. So par is another JavaScript renderer -- one built by the people who build the renderer.
 *
 * Par is also generous to Chrome twice over. Profiling instruments the very frontend being profiled, so the recorded
 * number carries the profiler's own overhead; and the viewer's expand-all pays DOM construction inside its figure,
 * where this rig bills construction to the People rung and the reveal only unfolds what is already built. Compare the
 * reveal against People + Reveal, never against the reveal alone.
 *
 * Keyed by what par is a property of -- the tree that was rendered -- because the three trees are of wildly different
 * sizes and a single number could only be par for one of them. Every pair is present, so [Map.getValue] is the right
 * lookup: a rung added without a par is a loud failure at startup rather than a quiet zero on the board.
 */
private val CHROME: Map<Pair<Dataset, Rung>, Double> = mapOf(
    (Dataset.SMOKE to Rung.DIVISIONS) to 18.0,
    (Dataset.SMOKE to Rung.GROUPS) to 59.0,
    (Dataset.SMOKE to Rung.TEAMS) to 66.0,
    (Dataset.SMOKE to Rung.PEOPLE) to 0.0,
    (Dataset.SMOKE to Rung.REVEAL) to 79.0,

    (Dataset.BENCH to Rung.DIVISIONS) to 19.0,
    (Dataset.BENCH to Rung.GROUPS) to 77.0,
    (Dataset.BENCH to Rung.TEAMS) to 79.0,
    (Dataset.BENCH to Rung.PEOPLE) to 0.0,
    (Dataset.BENCH to Rung.REVEAL) to 81.0,

    (Dataset.LOAD to Rung.DIVISIONS) to 21.0,
    (Dataset.LOAD to Rung.GROUPS) to 69.0,
    (Dataset.LOAD to Rung.TEAMS) to 136.0,
    (Dataset.LOAD to Rung.PEOPLE) to 0.0,
    (Dataset.LOAD to Rung.REVEAL) to 8152.0,
)

/**
 * The board every run is measured against. One row per dataset per rung -- fifteen -- is named for the demoscene type
 * rather than the index of nesting, so a row title says what was rendered without counting the four layers.
 *
 * The module columns start [BLANK]: nothing has been measured until a fixture reports.
 */
val PAR: Matrix = Matrix(
    // Par, then one column per module -- composed off of the enum so a column and the [Module.column]
    // a report is folded into cannot disagree about which framework owns which cell.
    columns = listOf("Par") + Module.entries.map { it.label },
    rows = Dataset.entries.flatMap { dataset ->
        Rung.entries.map { rung ->
            MatrixRow(
                dataset.key,
                rung.label,
                listOf(CHROME.getValue(dataset to rung).toString()) + Module.entries.map { BLANK },
            )
        }
    },
)
