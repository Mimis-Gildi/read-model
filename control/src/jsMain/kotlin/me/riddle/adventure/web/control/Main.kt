/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no remaining prototyping slop.
 */

package me.riddle.adventure.web.control

fun main() {
    Page.launch.addEventListener("click") { launchRun() }

    // Switching dataset is a view change.
    Page.dataset.addEventListener("change") { render(latest) }

    logger.info { "Ready" }
    render(null)
    connect()
}
