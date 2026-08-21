/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
package me.riddle.adventure.web.kobweb.fixture

import com.varabyte.kobweb.core.init.InitKobweb
import com.varabyte.kobweb.core.init.InitKobwebContext
import me.riddle.adventure.web.compose.core.ComposeRungFixture
import me.riddle.adventure.web.compose.core.wireFixtureClicks
import me.riddle.adventure.web.harness.start
import me.riddle.adventure.web.model.UI_FRAMEWORK_KOBWEB

/**
 * `:harness` [me.riddle.adventure.web.harness.Fixture] and body shared with `ComposeFixture.kt` via `:compose:core`.
 */
object KobwebFixture : ComposeRungFixture() {
    override val uiFramework: String = UI_FRAMEWORK_KOBWEB.first
}

/** Kobweb calls this once, before its own (empty) page composes into the hidden `#_kobweb-root`. */
@InitKobweb
fun initFixture(@Suppress("UNUSED_PARAMETER") ctx: InitKobwebContext) {
    wireFixtureClicks(KobwebFixture)
    start(KobwebFixture)
}
