/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals

class PerformanceComparisonTableTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }

        val prettyJson by lazy {
            Json {
                prettyPrint = true
                encodeDefaults = true
            }
        }

        /**
         * This breaks on contract change.
         */
        val exemplaryPerformanceComparisonTable by lazy {
            PerformanceComparisonTable(
                columns = listOf("Eeny", "Meeny", "Miny", "Moe"),
                rows = listOf(
                    PerformanceComparisonCategoryRow(
                        dataset = "Toe",
                        title = "Tiger",
                        cells = listOf(
                            PerformanceComparisonValues(listOf(1.347, 2.7392, 3.33333)),
                            PerformanceComparisonValues(listOf(4.347, 5.7392, 6.33333)),
                            PerformanceComparisonValues(listOf(7.347, 8.7392, 9.33333)),
                            PerformanceComparisonValues(listOf(10.347, 11.7392, 12.33333))
                        )
                    ),
                    PerformanceComparisonCategoryRow(
                        dataset = "Tail",
                        title = "Kitty",
                        cells = listOf(
                            PerformanceComparisonValues(listOf(13.347, 14.7392, 15.33333)),
                            PerformanceComparisonValues(listOf(16.347, 17.7392, 18.33333)),
                            PerformanceComparisonValues(listOf(19.347, 20.7392, 21.33333)),
                            PerformanceComparisonValues(listOf(22.347, 23.7392, 24.33333))
                        )
                    ),
                    PerformanceComparisonCategoryRow(
                        dataset = "Footsie",
                        title = "Lion",
                        cells = listOf(
                            PerformanceComparisonValues(),
                            PerformanceComparisonValues(),
                            PerformanceComparisonValues(),
                            PerformanceComparisonValues()
                        )
                    )
                ),
                totals = listOf(
                    PerformanceComparisonCategoryRow(
                        dataset = "Summed Up",
                        title = "Totals",
                        cells = listOf(
                            PerformanceComparisonValues(listOf(1.0, 2.0, 3.0, 4.0)),
                            PerformanceComparisonValues(),
                        )
                    ),
                    PerformanceComparisonCategoryRow(
                        dataset = "empty",
                        title = "Empty",
                        cells = emptyList()
                    )
                )
            )
        }
    }

    @Test
    fun `spot-test matrix`() {

        tLogger.debug { "State-lock matrix test - change this before the model." }

        val wireMatrix = prettyJson.encodeToString(exemplaryPerformanceComparisonTable)
        assertNotEquals("", wireMatrix, "Not nul but also not empty.")
        tLogger.trace { "PerformanceComparisonTable on the wire is:\n$wireMatrix" }

        assertContains(wireMatrix, "\"values\": []")
        assertContains(wireMatrix, "23.7392,")
    }
}
