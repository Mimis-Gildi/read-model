package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset

@Serializable
data class MatrixRow(val dataset: String, val title: String, val cells: List<String>)

@Serializable
data class Matrix(
    val type: String = "matrix",
    val columns: List<String>,
    val rows: List<MatrixRow>,
)

const val BLANK: String = " - "

fun Matrix.with(dataset: String, rung: Rung, column: Int, value: String): Matrix = copy(
    rows = rows.map { row ->
        when {
            row.dataset != dataset || row.title != rung.label -> row
            else -> row.copy(cells = row.cells.mapIndexed { at, cell ->
                when (at) {
                    column -> value; else -> cell
                }
            })
        }
    },
)


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
                listOf(CHROME.getOrDefault(dataset to rung, 0.0).toString()) + Module.entries.map { BLANK },
            )
        }
    },
)
