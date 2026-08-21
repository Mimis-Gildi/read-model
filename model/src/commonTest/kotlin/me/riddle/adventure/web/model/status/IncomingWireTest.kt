package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IncomingWireTest {

    val tLogger by lazy { KotlinLogging.logger {} }

    @Test
    fun `Report encoded via Incoming round-trips through Incoming`() {
        val report = Report(
            runId = "r1", uiFramework = "pure", datasetKey = "smoke",
            rung = "Teams", elements = 1, built = 1.0, painted = 1.0,
        )
        val wire = Json.encodeToString<Incoming>(report).also { tLogger.trace { "WIRE: $it" } }
        val decoded = Json.decodeFromString<Incoming>(wire)
        assertIs<Report>(decoded)
        assertEquals(report, decoded)
    }
}
