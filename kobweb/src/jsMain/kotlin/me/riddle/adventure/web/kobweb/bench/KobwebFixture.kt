package me.riddle.adventure.web.kobweb.bench

import me.riddle.adventure.web.harness.Fixture
import me.riddle.adventure.web.harness.start
import me.riddle.adventure.web.model.UI_FRAMEWORK_KOBWEB
import me.riddle.adventure.web.model.bench.Company

/** Step 0 probe: a Kotlin object implementing the harness's external `Fixture`, through the project dependency. */
object KobwebFixture : Fixture {
    override val uiFramework: String = UI_FRAMEWORK_KOBWEB.first
    override fun build(company: Company): Int = company.divisions.size
    override fun reset() {}
    override fun unfold(count: Int): Int = 0
    override fun fold(count: Int): Int = 0
}

/** Not called yet: proves `start` links from Kotlin. */
@Suppress("unused")
fun startKobwebFixture() = start(KobwebFixture)
