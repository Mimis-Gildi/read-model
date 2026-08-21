package me.riddle.adventure.web.service.data.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.riddle.adventure.web.model.bench.CorporateDivision.Companion.DIVISIONS
import me.riddle.adventure.web.model.bench.Dataset
import me.riddle.adventure.web.model.bench.Dataset.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTimedValue

class HumanResourcesDataServiceTest {

    companion object {

        val tLog = KotlinLogging.logger {}

        /**
         * Per class, not per test: JUnit builds a fresh test instance for every `@Test`, so an
         * instance-level `lazy` would regenerate all three companies once per method.
         */
        val defaultDataset by lazy {
            val (companies, initTime) = measureTimedValue {
                HumanResourcesDataService().run { Dataset.entries.associateWith { get(it) } }
            }
            if (initTime.inWholeMilliseconds > 40) tLog.warn { "Init Time is SLOW! ($initTime)" }

            companies
        }
    }

    @Test
    fun `get the smoke collection and measure init`() {
        mapOf(
            SMOKE to SMOKE.branchCountGroups * SMOKE.branchCountTeams * SMOKE.branchCountPeople * DIVISIONS.size,
            BENCH to BENCH.branchCountGroups * BENCH.branchCountTeams * BENCH.branchCountPeople * DIVISIONS.size,
            LOAD to LOAD.branchCountGroups * LOAD.branchCountTeams * LOAD.branchCountPeople * DIVISIONS.size
        )
            .map { (dataset, expected) ->
                expected to defaultDataset.getValue(dataset)
            }.map { (expected, company) ->
                (expected to company.dataset.key) to company.divisions
                    .flatMap { it.groups }.flatMap { it.teams }.flatMap { it.people }.count()
            }.forEach { (expected, actual) ->
                assertEquals(expected.first, actual, "Leaf reduction as the ${expected.first} final count for ${expected.second}")
                tLog.info { "Leaf reduction on  ${expected.second} has  the correct count of $actual people" }
            }
    }


    @Test
    fun `fable about datasets, load levels, and failure modes`() {
        Dataset.entries.map { it.nodes }.joinToString().also{ tLog.info{"Loading as $it"} }
    }
}
