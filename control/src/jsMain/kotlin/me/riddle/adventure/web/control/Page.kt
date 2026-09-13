/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no remaining prototyping slop.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.document
import me.riddle.adventure.web.model.PARAMETER_DATASET
import me.riddle.adventure.web.model.PARAMETER_UI_FRAMEWORK
import org.w3c.dom.*

/** Every element on the page resolves once against `control/index.html`. */
object Page {

    // @formatter:off
    val table: HTMLTableElement                                     = acquirePageHTMLElementById(PERFORMANCE_COMPARISON_TABLE)
    val performanceComparisonTableHeader: HTMLTableRowElement       = acquirePageHTMLElementById(PERFORMANCE_TABLE_HEADER)
    val performanceRowData: HTMLElement                             = acquirePageHTMLElementById(PERFORMANCE_ROW_DATA)
    val performanceComparisonTableFooter: HTMLElement               = acquirePageHTMLElementById(PERFORMANCE_COMPARISON_TABLE_FOOTER)
    val empty: HTMLElement                                          = acquirePageHTMLElementById(PERFORMANCE_TABLE_STATUS_MESSAGE)
    val connectionStatusContainer: HTMLElement                      = acquirePageHTMLElementById(CONNECTION_STATUS_COMPONENT)
    val connectionStatusText: HTMLElement                           = acquirePageHTMLElementById(CONNECTION_STATUS_TEXT)
    val fixtureTabLaunchStatus: HTMLElement                         = acquirePageHTMLElementById(FIXTURE_TAB_LAUNCH_STATUS)
    val uiFrameworkParameter: HTMLSelectElement                     = acquirePageHTMLElementById(PARAMETER_UI_FRAMEWORK)
    val datasetParameter: HTMLSelectElement                         = acquirePageHTMLElementById(PARAMETER_DATASET)
    val launchFixtureTest: HTMLElement                              = acquirePageHTMLElementById(LAUNCH_FIXTURE_TEST_TAB)
    // @formatter:on
}

private fun <T : Element> acquirePageHTMLElementById(id: String): T =
    document.getElementById(id)?.unsafeCast<T>() ?: error("control/index.html carries no #$id")

fun <T : Element> create(tag: String): T = document.createElement(tag).unsafeCast<T>()
