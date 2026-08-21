/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.service.data.status

import me.riddle.adventure.web.model.bench.Dataset
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.PerformanceComparisonValues
import me.riddle.adventure.web.model.status.PerformanceComparisonCategoryRow
import me.riddle.adventure.web.model.status.UIFrameworkUnderProfiling
import me.riddle.adventure.web.model.status.Report
import me.riddle.adventure.web.model.status.Rung
import me.riddle.adventure.web.model.status.TOTAL

fun Report.cell(): PerformanceComparisonValues = PerformanceComparisonValues(listOf(built, painted))

/** A partial column has no sum, methinks. */
private fun List<PerformanceComparisonValues>.summed(): PerformanceComparisonValues = when {
    isEmpty() || any { it.values.isEmpty() } -> PerformanceComparisonValues()
    else -> PerformanceComparisonValues(first().values.indices.map { at -> sumOf { it.values[at] } })
}

private fun PerformanceComparisonTable.totalled(): PerformanceComparisonTable = copy(
    totals = rows.groupBy(PerformanceComparisonCategoryRow::dataset).map { (dataset, group) ->
        PerformanceComparisonCategoryRow(dataset, TOTAL, columns.indices.map { at -> group.map { it.cells[at] }.summed() })
    },
)

fun PerformanceComparisonTable.with(dataset: String, rung: Rung, column: Int, value: PerformanceComparisonValues): PerformanceComparisonTable = copy(
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

/** Captured by histogram in debug utils, so high. */
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

/** Add PAR generated off of Chromes JSON View in Pretty-print. */
val PAR: PerformanceComparisonTable = PerformanceComparisonTable(
    columns = listOf("Par") + UIFrameworkUnderProfiling.entries.map { it.label },
    rows = Dataset.entries.flatMap { dataset ->
        Rung.entries.map { rung ->
            PerformanceComparisonCategoryRow(
                dataset.key,
                rung.label,
                listOf(PerformanceComparisonValues(listOf(CHROME.getOrDefault(dataset to rung, 0.0)))) + UIFrameworkUnderProfiling.entries.map { PerformanceComparisonValues() },
            )
        }
    },
).totalled()
