/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no remaining prototyping slop.
 */

package me.riddle.adventure.web.control

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.window
import kotlin.js.Date

val logger by lazy {  KotlinLogging.logger {} }

external fun encodeURIComponent(value: String): String

/** Some run differentiation. */
private fun runId() = Date().toISOString()

/** The mount is the module key. Routing.kt mounts fixture by key for now. */
fun launchRun() = with(runId()) {
    when {
        Page.module.value.isEmpty() -> logger.error { "UIFrameworkUnderProfiling is empty: Launch aborted!" }
        Page.dataset.value.isEmpty() -> logger.error { "Dataset is empty: Launch aborted!" }
        else -> window.open(
            "/${Page.module.value}/" +
                    "?run=${encodeURIComponent(this)}" +
                    "&module=${encodeURIComponent(Page.module.value)}" +
                    "&dataset=${encodeURIComponent(Page.dataset.value)}",
            "_blank",
        ).let {
            noteLaunched(Page.module.value, Page.dataset.value, this)
        }.also {
            logger.info { "Launched run $this" }
        }
    }
}
