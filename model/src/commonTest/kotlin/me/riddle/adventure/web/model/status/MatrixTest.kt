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

class MatrixTest {

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
        val exemplaryMatrix by lazy {
            Matrix(
                type = "exemplary matrix",
                columns = listOf("Eeny", "Meeny", "Miny", "Moe"),
                rows = listOf(
                    MatrixRow(
                        dataset = "Toe",
                        title = "Tiger",
                        cells = listOf(
                            MatrixCell(listOf(1.347, 2.7392, 3.33333)),
                            MatrixCell(listOf(4.347, 5.7392, 6.33333)),
                            MatrixCell(listOf(7.347, 8.7392, 9.33333)),
                            MatrixCell(listOf(10.347, 11.7392, 12.33333))
                        )
                    ),
                    MatrixRow(
                        dataset = "Tail",
                        title = "Kitty",
                        cells = listOf(
                            MatrixCell(listOf(13.347, 14.7392, 15.33333)),
                            MatrixCell(listOf(16.347, 17.7392, 18.33333)),
                            MatrixCell(listOf(19.347, 20.7392, 21.33333)),
                            MatrixCell(listOf(22.347, 23.7392, 24.33333))
                        )
                    ),
                    MatrixRow(
                        dataset = "Footsie",
                        title = "Lion",
                        cells = listOf(
                            MatrixCell(),
                            MatrixCell(),
                            MatrixCell(),
                            MatrixCell()
                        )
                    )
                ),
                totals = listOf(
                    MatrixRow(
                        dataset = "Summed Up",
                        title = "Totals",
                        cells = listOf(
                            MatrixCell(listOf(1.0, 2.0, 3.0, 4.0)),
                            MatrixCell(),
                        )
                    ),
                    MatrixRow(
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

        val wireMatrix = prettyJson.encodeToString(exemplaryMatrix)
        assertNotEquals("", wireMatrix, "Not nul but also not empty.")
        tLogger.trace { "Matrix on the wire is:\n$wireMatrix" }

        assertContains(wireMatrix, "\"values\": []")
        assertContains(wireMatrix, "23.7392,")
    }
}
