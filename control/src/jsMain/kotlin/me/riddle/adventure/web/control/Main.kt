/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no remaining prototyping slop.
 */

package me.riddle.adventure.web.control

import me.riddle.adventure.web.model.ON_CLICK

fun main() {
    Page.launchFixtureTest.addEventListener(ON_CLICK) { launchRun() }

    // Switching dataset is a view change.
    Page.datasetParameter.addEventListener("change") { render(performanceComparisonTable) }

    logger.info { "Ready" }
    render(null)
    connect()
}

const val CONNECTION_STATUS_COMPONENT = "connectionStatusComponent"
const val CONNECTION_STATUS_TEXT = "connectionStatusText"
const val LAUNCH_FIXTURE_TEST_TAB = "launchFixtureTestTab"
const val FIXTURE_TAB_LAUNCH_STATUS = "fixtureTabLaunchStatus"
const val PERFORMANCE_COMPARISON_TABLE = "performanceComparisonTable"
