/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.create
import me.riddle.adventure.web.model.ConnectionStatus
import me.riddle.adventure.web.model.HTML5_DATA_STATE
import me.riddle.adventure.web.control.ControlPage.connectionStatusContainer
import me.riddle.adventure.web.control.ControlPage.connectionStatusText
import me.riddle.adventure.web.control.ControlPage.fixtureTabLaunchStatus
import me.riddle.adventure.web.control.ControlPage.performanceComparisonTableFooter
import me.riddle.adventure.web.control.ControlPage.performanceComparisonTableHeader
import me.riddle.adventure.web.control.ControlPage.performanceRowData
import me.riddle.adventure.web.model.status.PerformanceComparisonCategoryRow
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.PerformanceComparisonValues
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTableRowElement
import kotlin.js.Date

private val launchHistory = mutableListOf<LaunchEvent>()

data class LaunchEvent(val framework: String, val dataset: String, val runId: String, val timestamp: Long)


/** The last performanceComparisonTable is always kept so the dataset picker can re-render. */
var performanceComparisonTable: PerformanceComparisonTable? = null

fun setConnectionStatusControl(status: ConnectionStatus) = connectionStatusContainer
    .apply { setAttribute(HTML5_DATA_STATE, status.dataState) }
    .also { connectionStatusText.textContent = status.label }
    .also { logger.info { "TBL: Connection state => $status: ${status.description}" } }

/** Fills one picker from the service, ignoring an empty vocabulary rather than blanking a working control. */
private fun HTMLSelectElement.fill(options: List<Pair<String, String>>) = options.onEach { (key, label) ->
    logger.info { "TBL: Filling <$id> option $key => $label" }
}.map { (slug, display) ->
    createNewTypedPageHTMLElement<HTMLOptionElement>(HTML_OPTION).apply {
        this@apply.value = slug
        textContent = display
    }
}.forEach { option -> appendChild(option) }

fun renderVocabulary(vocabulary: Vocabulary) {
    logger.info { "TBL: Vocabulary: $vocabulary" }
    ControlPage.uiFrameworkParameter.fill(vocabulary.frameworks.map { it.key to it.label })
    ControlPage.datasetParameter.fill(vocabulary.datasets.map { it.key to "${it.label} - ${countText(it.nodes)}" })
}

/** The rows for the dataset the user is looking at. */
private fun List<PerformanceComparisonCategoryRow>.forSelectedDataset(): List<PerformanceComparisonCategoryRow> = ControlPage.datasetParameter.value.let { selected ->
    logger.info { "TBL: Filtering rows for dataset '$selected'" }
    when {
        selected.isEmpty() -> this.also { logger.debug { "TBL: No dataset selected, returning all rows ($size)" } }
        else -> filter { it.dataset == selected }.also { logger.debug { "TBL: Returning ${it.size} rows for dataset '$selected'" } }
    }
}

/** Shared by the body and the totals foot. */
private fun rowElement(row: PerformanceComparisonCategoryRow, columns: List<String>, text: (PerformanceComparisonValues?) -> String): HTMLTableRowElement =
    columns.mapIndexed { at, name ->
        document.create.td {
            if (isPar(name)) classes += "par"
            +text(row.cells.getOrNull(at))
        }
    }.fold(document.create.tr {
        th {
            scope = ThScope.row
            +row.title
        }
    }) { tr, td -> tr.apply { appendChild(td) } } as HTMLTableRowElement


private fun headerCells(columns: List<String>) =
    listOf(document.create.th { scope = ThScope.col }) +
            columns.map { name ->
                document.create.th {
                    scope = ThScope.col
                    if (isPar(name)) classes += "par"
                    +name
                }
            }

private fun hideComparison() = ControlPage.performanceComparisonTable.apply { hidden = true }
    .also { ControlPage.connectionStatusComponent.style.display = "" }
    .also { logger.info { "TBL: Hiding Table - no data to render" } }

private fun showComparison(columns: List<String>, rows: List<PerformanceComparisonCategoryRow>, totals: List<PerformanceComparisonCategoryRow>) =
    ControlPage.performanceComparisonTable.apply { hidden = false }
        .also { ControlPage.connectionStatusComponent.style.display = "none" }
        .also { headerCells(columns).fold(performanceComparisonTableHeader) { tr, th -> tr.apply { appendChild(th) } } }
        .also { rows.fold(performanceRowData) { body, row -> body.apply { appendChild(rowElement(row, columns, ::cellText)) } } }
        .also { totals.fold(performanceComparisonTableFooter) { foot, row -> foot.apply { appendChild(rowElement(row, columns, ::totalText)) } } }


/** Renders whatever arrived as it arrived. */
fun render(table: PerformanceComparisonTable?) =
    listOf(performanceComparisonTableHeader, performanceRowData, performanceComparisonTableFooter)
        .onEach { it.textContent = "" }
        .run {
            Triple(
                table?.columns.orEmpty(),
                table?.rows.orEmpty().forSelectedDataset(),
                table?.totals.orEmpty().forSelectedDataset(),
            )
        }.let { (columns, rows, totals) ->
            when {
                columns.isEmpty() || rows.isEmpty() -> hideComparison()
                else -> showComparison(columns, rows, totals)
            }
        }


fun noteLaunched(uiFixtureId: String, dataset: String, runId: String) =
    launchHistory.apply {
        add(LaunchEvent(uiFixtureId, dataset, runId, Date.now().toLong()))
    }.toList().run(::renderLaunchHistory)


private fun renderLaunchHistory(events: List<LaunchEvent>) =
    events.sortedBy(LaunchEvent::timestamp)
        .takeLast(5)
        .map { (framework, datasetName, runId, timestamp) ->
            document.create.li {
                +"${Date(timestamp).toLocaleString()}:: "
                b { +framework }
                +": "
                i { +datasetName }
                +" (..${runId.takeLast(7)})"
            }
        }.fold(document.create.ul {}) { ul, li -> ul.apply { appendChild(li) } }
        .run { fixtureTabLaunchStatus.apply { textContent = "" }.apply { appendChild(this@run) } }



