/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.document
import kotlinx.html.b
import kotlinx.html.dom.create
import kotlinx.html.i
import kotlinx.html.js.li
import kotlinx.html.js.ul
import me.riddle.adventure.web.control.ControlPage.connectionStatusContainer
import me.riddle.adventure.web.control.ControlPage.connectionStatusText
import me.riddle.adventure.web.control.ControlPage.fixtureTabLaunchStatus
import me.riddle.adventure.web.model.status.PerformanceComparisonCategoryRow
import me.riddle.adventure.web.model.status.PerformanceComparisonTable
import me.riddle.adventure.web.model.status.PerformanceComparisonValues
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableRowElement
import kotlin.js.Date

private val launchHistory = mutableListOf<LaunchEvent>()

data class LaunchEvent(val framework: String, val dataset: String, val runId: String, val timestamp: Long)


/** The last performanceComparisonTable is always kept so the dataset picker can re-render. */
var performanceComparisonTable: PerformanceComparisonTable? = null

fun setConnectionStatusControl(state: String, text: String) {
    logger.info { "TBL: Connection state => $state : $text" }
    connectionStatusContainer.setAttribute(HTML5_DATA_STATE, state)
    connectionStatusText.textContent = text
}

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
    logger.info { "TBL: Vocabulary: ${vocabulary.modules.size} modules, ${vocabulary.datasets.size} datasets" }
    ControlPage.uiFrameworkParameter.fill(vocabulary.modules.map { it.key to it.label })
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
    createNewTypedPageHTMLElement<HTMLTableRowElement>("tr").apply {

        appendChild(createNewTypedPageHTMLElement<HTMLTableCellElement>("th").apply {
            scope = "row"
            textContent = row.title
        }).also { logger.debug { "TBL: Created row element titled '${row.title}'" } }

        columns.forEachIndexed { at, name ->
            appendChild(createNewTypedPageHTMLElement<HTMLTableCellElement>("td").apply {
                if (isPar(name)) className = "par"
                textContent = text(row.cells.getOrNull(at))
            }).also { logger.debug { "TBL: Created cell element for '${row.title}' at column $at" } }
        }
    }

/** Renders whatever arrived as it arrived. */
fun render(performanceComparisonTable: PerformanceComparisonTable?) {
    logger.info { "TBL: Rendering performanceComparisonTable ..." }

    ControlPage.performanceComparisonTableHeader.textContent = ""
    ControlPage.performanceRowData.textContent = ""
    ControlPage.performanceComparisonTableFooter.textContent = ""

    val columns = performanceComparisonTable?.columns.orEmpty()
    val rows = performanceComparisonTable?.rows.orEmpty().forSelectedDataset()
    val totals = performanceComparisonTable?.totals.orEmpty().forSelectedDataset()

    when {
        columns.isEmpty() || rows.isEmpty() -> {
            logger.info { "TBL: Hiding Table - no performanceComparisonTable data to render" }
            ControlPage.performanceComparisonTable.hidden = true
            ControlPage.connectionStatusComponent.style.display = ""
        }

        else -> {
            logger.info { "TBL: Rendering performanceComparisonTable with ${columns.size} columns and ${rows.size} rows." }
            ControlPage.performanceComparisonTable.hidden = false
            ControlPage.connectionStatusComponent.style.display = "none"

            ControlPage.performanceComparisonTableHeader.appendChild(createNewTypedPageHTMLElement<HTMLTableCellElement>("th").apply {
                className = "corner"
                scope = "col"
            })
            columns.forEach { name ->
                ControlPage.performanceComparisonTableHeader.appendChild(createNewTypedPageHTMLElement<HTMLTableCellElement>("th").apply {
                    scope = "col"
                    if (isPar(name)) className = "par"
                    textContent = name
                }).also { logger.debug { "TBL: Created column header element for '$name'" } }
            }

            rows.forEach { ControlPage.performanceRowData.appendChild(rowElement(it, columns, ::cellText)) }
            totals.forEach { ControlPage.performanceComparisonTableFooter.appendChild(rowElement(it, columns, ::totalText)) }
        }
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



