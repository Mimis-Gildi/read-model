/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.control

import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.PerformanceComparisonValues
import me.riddle.adventure.web.model.status.PerformanceComparisonCategoryRow
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.*

/** The last performanceComparisonTable is always kept so the dataset picker can re-render. */
var performanceComparisonTable: PerformanceComparisonTable? = null

fun setConnectionStatusControl(state: String, text: String) {
    logger.info { "Status-PerformanceComparisonTable: Setting connection state to $state and $text" }
    Page.connectionStatusContainer.setAttribute("data-state", state)
    Page.connectionStatusText.textContent = text
}

/** Fills one picker from the service, ignoring an empty vocabulary rather than blanking a working control. */
private fun HTMLSelectElement.fill(options: List<Pair<String, String>>) {
    if (options.isEmpty()) {
        logger.warn { "Status-PerformanceComparisonTable: Ignoring empty vocabulary: $options is empty." }
        return
    }

    val previous = value
    if (value.isNotEmpty()) logger.info { "Status-PerformanceComparisonTable: Previous value: $previous" }

    textContent = ""

    options.forEach { (key, label) ->
        appendChild(create<HTMLOptionElement>("option").apply {
            this.value = key
            textContent = label
        }.also {
            logger.info { "Status-PerformanceComparisonTable: Writing option: ${it.value} -> '${it.text}'" }
        })
    }

    // The socket re-sends on every reconnection. Dropped connection must not matter.
    // Empty `previous` selects what is already there.
    value = previous.ifEmpty { value }
}

fun renderVocabulary(vocabulary: Vocabulary) {
    logger.info { "Status-PerformanceComparisonTable: Rendering vocabulary: ${vocabulary.modules.size} modules, ${vocabulary.datasets.size} datasets" }
    Page.uiFrameworkParameter.fill(vocabulary.modules.map { it.key to it.label })
    Page.datasetParameter.fill(vocabulary.datasets.map { it.key to "${it.label} - ${countText(it.nodes)}" })
}

/** The rows for the dataset the user is looking at. */
private fun List<PerformanceComparisonCategoryRow>.forSelectedDataset(): List<PerformanceComparisonCategoryRow> = Page.datasetParameter.value.let { selected ->
    logger.info { "Status-PerformanceComparisonTable: Filtering rows for dataset '$selected'" }
    when {
        selected.isEmpty() -> this.also { logger.debug { "Status-PerformanceComparisonTable: No dataset selected, returning all rows ($size)" } }
        else -> filter { it.dataset == selected }.also { logger.debug { "Status-PerformanceComparisonTable: Returning ${it.size} rows for dataset '$selected'" } }
    }
}

/** Shared by the body and the totals foot. */
private fun rowElement(row: PerformanceComparisonCategoryRow, columns: List<String>, text: (PerformanceComparisonValues?) -> String): HTMLTableRowElement =
    create<HTMLTableRowElement>("tr").apply {

        appendChild(create<HTMLTableCellElement>("th").apply {
            scope = "row"
            textContent = row.title
        }).also { logger.debug { "Status-PerformanceComparisonTable: Created row element titled '${row.title}'" } }

        columns.forEachIndexed { at, name ->
            appendChild(create<HTMLTableCellElement>("td").apply {
                if (isPar(name)) className = "par"
                textContent = text(row.cells.getOrNull(at))
            }).also { logger.debug { "Status-PerformanceComparisonTable: Created cell element for '${row.title}' at column $at" } }
        }
    }

/** Renders whatever arrived as it arrived. */
fun render(performanceComparisonTable: PerformanceComparisonTable?) {
    logger.info { "Status-PerformanceComparisonTable: Rendering performanceComparisonTable ..." }

    Page.head.textContent = ""
    Page.rows.textContent = ""
    Page.foot.textContent = ""

    val columns = performanceComparisonTable?.columns.orEmpty()
    val rows = performanceComparisonTable?.rows.orEmpty().forSelectedDataset()
    val totals = performanceComparisonTable?.totals.orEmpty().forSelectedDataset()

    when {
        columns.isEmpty() || rows.isEmpty() -> {
            logger.info { "Status-PerformanceComparisonTable: Hiding Table - no performanceComparisonTable data to render" }
            Page.table.hidden = true
            Page.empty.style.display = ""
        }

        else -> {
            logger.info { "Status-PerformanceComparisonTable: Rendering performanceComparisonTable with ${columns.size} columns and ${rows.size} rows." }
            Page.table.hidden = false
            Page.empty.style.display = "none"

            Page.head.appendChild(create<HTMLTableCellElement>("th").apply {
                className = "corner"
                scope = "col"
            })
            columns.forEach { name ->
                Page.head.appendChild(create<HTMLTableCellElement>("th").apply {
                    scope = "col"
                    if (isPar(name)) className = "par"
                    textContent = name
                }).also { logger.debug { "Status-PerformanceComparisonTable: Created column header element for '$name'" } }
            }

            rows.forEach { Page.rows.appendChild(rowElement(it, columns, ::cellText)) }
            totals.forEach { Page.foot.appendChild(rowElement(it, columns, ::totalText)) }
        }
    }
}

/** The launch note names the run in the short form the performanceComparisonTable and the logs both use. */
fun noteLaunched(moduleId: String, dataset: String, run: String) {
    Page.fixtureTabLaunchStatus.textContent = "launched $moduleId / $dataset as run "
    Page.fixtureTabLaunchStatus.appendChild(create<HTMLElement>("b").apply { textContent = run })
}
