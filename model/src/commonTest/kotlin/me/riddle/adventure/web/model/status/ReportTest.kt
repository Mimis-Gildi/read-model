/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.DATASET_KEY_SMOKE
import me.riddle.adventure.web.model.UI_FRAMEWORK_PURE_TS
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }

        val report by lazy {
            Report(
                runId = "1787955391293-5501f6aa1128b8",
                uiFramework = UI_FRAMEWORK_PURE_TS.first,
                datasetKey = DATASET_KEY_SMOKE,
                rung = Rung.TEAMS.label,
                elements = 2200,
                built = 12.5,
                painted = 3.25
            )
        }

        val reportOnTheWire by lazy {
            """
                {
                  "type": "report",
                  "runId": "1787955391293-5501f6aa1128b8",
                  "uiFramework": "pure",
                  "datasetKey": "smoke",
                  "rung": "Teams",
                  "elements": 2200,
                  "built": 12.5,
                  "painted": 3.25
                }
            """.trimIndent()
        }

        val prettyJson by lazy {
            Json {
                prettyPrint = true
                prettyPrintIndent = "  "
                encodeDefaults = true
            }
        }
    }

    @Test
    fun `should serialize with defaulted type`() = assertEquals(
        reportOnTheWire,
        prettyJson.encodeToString(report).also { tLogger.trace { "Report is:\n\n$it" } },
        "Type defaults to 'report' and every field is on the wire"
    )

    @Test
    fun `should deserialize hand-json to report`() = assertEquals(
        report,
        Json.decodeFromString<Report>(reportOnTheWire),
        "Hand-written JSON round-trips to the same report"
    )
}
