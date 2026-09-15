/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import io.github.oshai.kotlinlogging.KotlinLogging
import me.riddle.adventure.web.model.ON_CHANGE
import me.riddle.adventure.web.model.ON_CLICK

fun main() {
    logger.info { "Initializing Control Plane UI." }
    ControlPage.launchFixtureTest.addEventListener(ON_CLICK) { launchNewBenchmarkRunTab() }
        .also { logger.info { "Registered Fixture Tab LaunchEvent action button." } }

    // Switching dataset is a view change.
    ControlPage.datasetParameter.addEventListener(ON_CHANGE) { render(performanceComparisonTable) }
        .also { logger.info { "Registered 'On Change' event listener to Render." } }

    logger.info { "Control Plane Application is Ready for the user actions." }
    render(null).also { logger.info { "Rendered Control Plane UI." } }
    connect().also { logger.info { "Called Event Bus." } }
}

// @formatter:off
const val CONNECTION_STATUS_COMPONENT           = "connectionStatusComponent"
const val CONNECTION_STATUS_TEXT                = "connectionStatusText"
const val LAUNCH_FIXTURE_TEST_TAB               = "launchFixtureTestTab"
const val FIXTURE_TAB_LAUNCH_STATUS             = "fixtureTabLaunchStatus"
const val PERFORMANCE_COMPARISON_TABLE          = "performanceComparisonTable"
const val PERFORMANCE_TABLE_HEADER              = "performanceTableHeader"
const val PERFORMANCE_ROW_DATA                  = "performanceRowData"
const val PERFORMANCE_COMPARISON_TABLE_FOOTER   = "performanceComparisonTableFooter"
const val PERFORMANCE_TABLE_STATUS_MESSAGE      = "performanceTableStatusMessage"

const val NEW_TAB                               = "_blank"

const val HTML_OPTION                           = "option"
// @formatter:on

val logger by lazy { KotlinLogging.logger {} }
