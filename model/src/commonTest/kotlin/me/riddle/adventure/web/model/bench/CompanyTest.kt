/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored - interpolated from hands-written examples and refactored.
 */

package me.riddle.adventure.web.model.bench

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.riddle.adventure.web.model.bench.CorporateDivision.Companion.DIVISIONS
import me.riddle.adventure.web.model.bench.CorporateDivision.Companion.DIVISIONS_ALL
import me.riddle.adventure.web.model.bench.CorporateGroup.Companion.GROUPS
import me.riddle.adventure.web.model.bench.ProductTeam.Companion.TEAMS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyTest {

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }

        val company by lazy {
            Company(
                Dataset.SMOKE,
                divisions = listOf(
                    CorporateDivision.of(
                        id = 0,
                        corporateGroups = listOf(
                            CorporateGroup.of(
                                id = 0,
                                productTeams = listOf(
                                    ProductTeam.of(
                                        id = 0,
                                        teamMembers = listOf(// @formatter:off
                                            Person.of(index = 0))))))))) }
        // @formatter:on
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
        Dataset.FAIL to 1_048_749,
    ).forEach { (dataset, nodes) ->
        assertEquals(
            expected = nodes,
            actual = dataset.nodes
                .also { tLogger.info { "${dataset.key} is $it nodes" } },
            message = "${DIVISIONS.size} divisions of branches"
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

    @Test
    fun `base divisions is omnipresent`() {
        val (divId, division, groups) = CorporateDivision.of(id = 7, corporateGroups = listOf())
        assertEquals(7, divId)
        assertTrue(groups.isEmpty())
        assertEquals("Marketing", division)

        val (gId, group, teams) = CorporateGroup.of(id = 7, productTeams = listOf())
        assertEquals(7, gId)
        assertTrue(teams.isEmpty())
        assertEquals("Networking 7", group)

        val (tId, team, people) = ProductTeam.of(id = 7, teamMembers = listOf())
        assertEquals(7, tId)
        assertTrue(people.isEmpty())
        assertEquals("Harbor 7", team)

        val (id, firstName, lastName, jobTitle, location, phone) = Person.of(index = 711)
        assertEquals(expected = 711, actual = id, message = "Must be a cop?")
        assertEquals(expected = "James", actual = firstName, message = "Not Billy?")
        assertEquals(expected = "Mendoza", actual = lastName, message = "Whole day")
        assertEquals(expected = "UX Designer", actual = jobTitle, message = "Because it's pretty and doesn't pop the Owl!")
        assertEquals(expected = "Remote", actual = location, message = "But of course")
        assertEquals(expected = "(550) 557-6936", actual = phone, message = "Call! Who you get?")

        tLogger.info { "Generators are always to be trusted, if Joe Random Hacker is." }

        // @formatter:off
        @Suppress("JSON_FORMAT_REDUNDANT")
        val liferJamesOfHarborTeamNetworkingInMarketing = Json { prettyPrint = true }
            .encodeToString<CorporateDivision>(
                value = CorporateDivision.of(id = 7, corporateGroups = listOf(
                    CorporateGroup.of(id = 7, productTeams = listOf(
                        ProductTeam.of(id = 7, teamMembers = listOf(
                            Person.of(index = 711))))))))
        // @formatter:on

        val james = Json.parseToJsonElement(liferJamesOfHarborTeamNetworkingInMarketing)

        tLogger.info { "But if you're a hacker, you can't trust anyone." }
        assertEquals("Marketing", james.jsonObject["division"]?.jsonPrimitive?.content ?: "Kitchen", "Not kitchen.")
        assertEquals(
            "Networking 7", james.jsonObject["groups"]
                ?.jsonArray?.get(0)?.jsonObject?.get("group")?.jsonPrimitive?.content ?: "Goths", "Not Goths."
        )
        val jamesDivision = Json.decodeFromString<CorporateDivision>(james.toString())
        assertEquals("James", jamesDivision.groups[0].teams[0].people[0].firstName, "Can't trust James.")

        assertEquals(36, DIVISIONS_ALL.size, "Because Acme is big.")
        assertEquals(13, DIVISIONS.size, "But not all count.")
        assertEquals(20, GROUPS.size, "And there are groups.")
        assertEquals(24, TEAMS.size, "And even teams.")
    }

}
