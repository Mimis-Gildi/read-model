/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
package me.riddle.adventure.web.compose

import me.riddle.adventure.web.compose.core.ComposeRungFixture
import me.riddle.adventure.web.compose.core.wireFixtureClicks
import me.riddle.adventure.web.harness.start
import me.riddle.adventure.web.model.UI_FRAMEWORK_COMPOSE

/**
 * :kobweb sibling, minus Kobweb: `androidx.compose.web` composer core from `:compose:core`.
 */
object ComposeFixture : ComposeRungFixture() {
    override val uiFramework: String = UI_FRAMEWORK_COMPOSE.first
}

fun main() {
    wireFixtureClicks(ComposeFixture)
    start(ComposeFixture)
}
