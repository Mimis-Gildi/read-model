package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset

/**
 * Each row measurement is the captured numbers to be added up in a column.
 * Formatting happens in the page, which is the only reader, so every column rounds the same way.
 */
@Serializable
data class MatrixCell(val values: List<Double> = emptyList())

@Serializable
data class MatrixRow(val dataset: String, val title: String, val cells: List<MatrixCell>)

@Serializable
data class Matrix(
    val type: String = "matrix",
    val columns: List<String>,
    val rows: List<MatrixRow>,
    val totals: List<MatrixRow> = emptyList(),
)

const val TOTAL: String = "Total"

/** A partial column has no honest sum, so one absent cell blanks the whole total. */
private fun List<MatrixCell>.summed(): MatrixCell = when {
    isEmpty() || any { it.values.isEmpty() } -> MatrixCell()
    else -> MatrixCell(first().values.indices.map { at -> sumOf { it.values[at] } })
}

private fun Matrix.totalled(): Matrix = copy(
    totals = rows.groupBy(MatrixRow::dataset).map { (dataset, group) ->
        MatrixRow(dataset, TOTAL, columns.indices.map { at -> group.map { it.cells[at] }.summed() })
    },
)

fun Matrix.with(dataset: String, rung: Rung, column: Int, value: MatrixCell): Matrix = copy(
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
).totalled()

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
 * Add PAR generated off of Chromes JSON View in Pretty-print.
 */
val PAR: Matrix = Matrix(
    columns = listOf("Par") + Module.entries.map { it.label },
    rows = Dataset.entries.flatMap { dataset ->
        Rung.entries.map { rung ->
            MatrixRow(
                dataset.key,
                rung.label,
                listOf(MatrixCell(listOf(CHROME.getOrDefault(dataset to rung, 0.0)))) + Module.entries.map { MatrixCell() },
            )
        }
    },
).totalled()
