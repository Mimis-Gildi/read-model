/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.document
import me.riddle.adventure.web.model.PARAMETER_DATASET
import me.riddle.adventure.web.model.PARAMETER_UI_FRAMEWORK
import org.w3c.dom.*

/** Every element on the page resolves once against `control/index.html`. */
object ControlPage {

    // @formatter:off
    val performanceComparisonTable: HTMLTableElement                = acquirePageHTMLElementById(PERFORMANCE_COMPARISON_TABLE)
    val performanceComparisonTableHeader: HTMLTableRowElement       = acquirePageHTMLElementById(PERFORMANCE_TABLE_HEADER)
    val performanceRowData: HTMLTableSectionElement                 = acquirePageHTMLElementById(PERFORMANCE_ROW_DATA)
    val performanceComparisonTableFooter: HTMLTableSectionElement   = acquirePageHTMLElementById(PERFORMANCE_COMPARISON_TABLE_FOOTER)
    val connectionStatusComponent: HTMLDivElement                   = acquirePageHTMLElementById(PERFORMANCE_TABLE_STATUS_MESSAGE)
    val connectionStatusContainer: HTMLDivElement                   = acquirePageHTMLElementById(CONNECTION_STATUS_COMPONENT)
    val connectionStatusText: HTMLSpanElement                       = acquirePageHTMLElementById(CONNECTION_STATUS_TEXT)
    val fixtureTabLaunchStatus: HTMLDivElement                      = acquirePageHTMLElementById(FIXTURE_TAB_LAUNCH_STATUS)
    val uiFrameworkParameter: HTMLSelectElement                     = acquirePageHTMLElementById(PARAMETER_UI_FRAMEWORK)
    val datasetParameter: HTMLSelectElement                         = acquirePageHTMLElementById(PARAMETER_DATASET)
    val launchFixtureTest: HTMLButtonElement                        = acquirePageHTMLElementById(LAUNCH_FIXTURE_TEST_TAB)
    // @formatter:on
}

/** Type-safe and compiler-checked element acquisition at runtime. */
private inline fun <reified T : Element> acquirePageHTMLElementById(id: String): T =
    (document.getElementById(id) ?: error("control/index.html carries no #$id")).run {
        this as? T ?: error("Element #$id is of type ${this::class.simpleName}, but expected ${T::class.simpleName}")
    }

/** Smart-cast checked at compilation way of getting. */
inline fun <reified T : Element> createNewTypedPageHTMLElement(htmlTag: String): T =
    document.createElement(htmlTag).run {
        this as? T ?: error("Tag '$htmlTag' does not produce an instance of ${T::class.simpleName}")
    }
