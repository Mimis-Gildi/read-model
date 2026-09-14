/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.window
import me.riddle.adventure.web.model.DEFAULT_UI_FRAMEWORK
import me.riddle.adventure.web.model.PARAMETER_DATASET
import me.riddle.adventure.web.model.PARAMETER_RUN_ID
import kotlin.js.Date

external fun encodeURIComponent(value: String): String

/** The mount is the module key. Routing.kt mounts fixture by key for now. */
fun launchNewBenchmarkRunTab() = with(ControlPage) {
    (uiFrameworkParameter.value.ifEmpty { DEFAULT_UI_FRAMEWORK } to encodeURIComponent(Date().toISOString())).run {
        val dataset = datasetParameter.value
        logger.info { "LaunchEvent Invocations as $this - $dataset" }
        window.open(
            url = buildString {
                append("/$first/")
                append("?$PARAMETER_RUN_ID=$second")
                append("&$PARAMETER_DATASET=$dataset")
            },
            target = NEW_TAB,
        ).run {
            noteLaunched(first, dataset, second)
        }.also {
            logger.info { "Launched run $this" }
        }
    }
}
