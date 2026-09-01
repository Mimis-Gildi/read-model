/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Refactored: 80%.
 * The remainder is validated prototyping slop,
 *   provisionally accepted and temporary.
 *   Needs imperative refactoring out.
 */

package me.riddle.adventure.web.control

import me.riddle.adventure.web.model.status.Matrix
import me.riddle.adventure.web.model.status.MatrixCell
import me.riddle.adventure.web.model.status.MatrixRow
import me.riddle.adventure.web.model.status.Vocabulary
import org.w3c.dom.*

/** The last matrix is always kept so the dataset picker can re-render. */
var latest: Matrix? = null

fun setConn(state: String, text: String) {
    logger.info { "Status-Matrix: Setting connection state to $state and $text" }
    Page.conn.setAttribute("data-state", state)
    Page.connText.textContent = text
}

/** Fills one picker from the service, ignoring an empty vocabulary rather than blanking a working control. */
private fun HTMLSelectElement.fill(options: List<Pair<String, String>>) {
    if (options.isEmpty()) {
        logger.warn { "Status-Matrix: Ignoring empty vocabulary: $options is empty." }
        return
    }

    val previous = value
    if (value.isNotEmpty()) logger.info { "Status-Matrix: Previous value: $previous" }

    textContent = ""

    options.forEach { (key, label) ->
        appendChild(create<HTMLOptionElement>("option").apply {
            this.value = key
            textContent = label
        }.also {
            logger.info { "Status-Matrix: Writing option: ${it.value} -> '${it.text}'" }
        })
    }

    // The socket re-sends on every reconnection. Dropped connection must not matter.
    // Empty `previous` selects what is already there.
    value = previous.ifEmpty { value }
}

fun renderVocabulary(vocabulary: Vocabulary) {
    logger.info { "Status-Matrix: Rendering vocabulary '${vocabulary.type}': ${vocabulary.modules.size} modules, ${vocabulary.datasets.size} datasets" }
    Page.module.fill(vocabulary.modules.map { it.key to it.label })
    Page.dataset.fill(vocabulary.datasets.map { it.key to "${it.label} - ${countText(it.nodes)}" })
}

/** The rows for the dataset the user is looking at. */
private fun List<MatrixRow>.forSelectedDataset(): List<MatrixRow> = Page.dataset.value.let { selected ->
    logger.info { "Status-Matrix: Filtering rows for dataset '$selected'" }
    when {
        selected.isEmpty() -> this.also { logger.debug { "Status-Matrix: No dataset selected, returning all rows ($size)" } }
        else -> filter { it.dataset == selected }.also { logger.debug { "Status-Matrix: Returning ${it.size} rows for dataset '$selected'" } }
    }
}

/** Shared by the body and the totals foot. */
private fun rowElement(row: MatrixRow, columns: List<String>, text: (MatrixCell?) -> String): HTMLTableRowElement =
    create<HTMLTableRowElement>("tr").apply {

        appendChild(create<HTMLTableCellElement>("th").apply {
            scope = "row"
            textContent = row.title
        }).also { logger.debug { "Status-Matrix: Created row element titled '${row.title}'" } }

        columns.forEachIndexed { at, name ->
            appendChild(create<HTMLTableCellElement>("td").apply {
                if (isPar(name)) className = "par"
                textContent = text(row.cells.getOrNull(at))
            }).also { logger.debug { "Status-Matrix: Created cell element for '${row.title}' at column $at" } }
        }
    }

/** Renders whatever arrived as it arrived. */
fun render(matrix: Matrix?) {
    logger.info { "Status-Matrix: Rendering matrix ..." }

    Page.head.textContent = ""
    Page.rows.textContent = ""
    Page.foot.textContent = ""

    val columns = matrix?.columns.orEmpty()
    val rows = matrix?.rows.orEmpty().forSelectedDataset()
    val totals = matrix?.totals.orEmpty().forSelectedDataset()

    when {
        columns.isEmpty() || rows.isEmpty() -> {
            logger.info { "Status-Matrix: Hiding Table - no matrix data to render" }
            Page.table.hidden = true
            Page.empty.style.display = ""
        }

        else -> {
            logger.info { "Status-Matrix: Rendering matrix with ${columns.size} columns and ${rows.size} rows." }
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
                }).also { logger.debug { "Status-Matrix: Created column header element for '$name'" } }
            }

            rows.forEach { Page.rows.appendChild(rowElement(it, columns, ::cellText)) }
            totals.forEach { Page.foot.appendChild(rowElement(it, columns, ::totalText)) }
        }
    }
}

/** The launch note names the run in the short form the matrix and the logs both use. */
fun noteLaunched(moduleId: String, dataset: String, run: String) {
    Page.launched.textContent = "launched $moduleId / $dataset as run "
    Page.launched.appendChild(create<HTMLElement>("b").apply { textContent = run })
}
