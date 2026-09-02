/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored - interpolated from hands-written examples and refactored.
 */

package me.riddle.adventure.web.model.bench

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.bench.CorporateGroup.Companion.of
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }

        val company by lazy {
            Company(
                Dataset.SMOKE,
                listOf(
                    CorporateDivision.of(
                        id = 0, corporateGroups = listOf(
                            of(
                                id = 0, productTeams = listOf(
                                    ProductTeam.of(
                                        id = 0, teamMembers = listOf(
                                            Person.of(index = 0)
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
            )
        }
    }

    @Test
    fun `should resolve dataset by url key`() = mapOf(
        "smoke" to Dataset.SMOKE,
        "bench" to Dataset.BENCH,
        "load" to Dataset.LOAD,
        "boom" to Dataset.FAIL,
        "fail" to null,
        "" to null,
        null to null,
    ).forEach { (key, dataset) ->
        assertEquals(dataset, Dataset.of(key), "The wire never sees the constant name")
    }

    @Test
    fun `should count every node in the tree`() = mapOf(
        Dataset.SMOKE to 1_469,
        Dataset.BENCH to 18_148,
        Dataset.LOAD to 320_073,
        Dataset.FAIL to 2_124_148,
    ).forEach { (dataset, nodes) ->
        assertEquals(
            expected = nodes,
            actual = dataset.nodes
                .also { tLogger.info { "${dataset.key} is $it nodes" } },
            message = "${CorporateDivision.DIVISIONS.size} divisions of branches"
        )
    }

    @Test
    fun `quick de-and-serializer test`() = assertEquals(
        expected = company,
        actual = Json.decodeFromString<Company>(
            Json.encodeToString(company)
                .also { tLogger.trace { "Company is: $it" } }),
        message = "The whole tree survives the wire"
    )
}
