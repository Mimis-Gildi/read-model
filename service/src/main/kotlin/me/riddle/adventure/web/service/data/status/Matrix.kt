package me.riddle.adventure.web.service.data.status

import me.riddle.adventure.web.model.status.Matrix
import me.riddle.adventure.web.model.status.MatrixCell
import me.riddle.adventure.web.model.status.MatrixRow
import me.riddle.adventure.web.model.status.TOTAL
import me.riddle.adventure.web.service.data.bench.Dataset

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
