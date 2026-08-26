package me.riddle.adventure.web.service.data.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Dataset.*
import me.riddle.adventure.web.service.data.bench.Person
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
        mapOf(SMOKE to 768, BENCH to 15_972, LOAD to 187_500).map { (dataset, expected) ->
            expected to defaultDataset[dataset]!!
        }.map { (expected, company) ->

            (expected to company.dataset.key) to company.divisions
                .flatMap { it.groups }.flatMap { it.teams }.flatMap { it.people }.count()
        }.forEach { (expected, actual) ->
            assertEquals(expected.first, actual, "Leaf reduction as the ${expected.first} final count for ${expected.second}")
            tLog.info { "Leaf reduction on  ${expected.second} has  the correct count of $actual people" }
        }
    }

    @Test
    fun `spot-check our favorite people in their expected positions`() {
        assertEquals(
            Person(
                0,
                "Mark",
                "Carter",
                "Support Specialist",
                "Phoenix, AZ",
                "(298) 245-1653"
            ),

            defaultDataset[SMOKE]!!.divisions[0].groups[0].teams[0].people[0],
            "First smoke object is a mismatch"
        )
        tLog.info { "Mark Carter is still employed with the first team." }

        val folksOfUmbra = defaultDataset[BENCH]!!.divisions.asSequence()
            .filter { it.division == "Product" }.flatMap { it.groups }
            .filter { it.group == "Machine Learning 12" }.flatMap { it.teams }
            .filter { it.team == "Umbra 140" }.flatMap { it.people }
            .map { person: Person -> "${person.lastName}, ${person.firstName}" }.toSet()

        assertEquals(11, folksOfUmbra.size)
        assertTrue {
            folksOfUmbra.containsAll(
                setOf(
                    "Price, Sarah", "Edwards, Donna", "Bennett, Andrew", "Castillo, Ryan",
                    "Kim, Melissa", "Mendoza, Gregory", "Brown, Linda", "Miller, Justin",
                    "Foster, Steven", "Brooks, Brian", "Rivera, Anthony"
                )
            )
        }
        tLog.info { "The Umbra team is still intact." }

        val folksOfAtlas = defaultDataset[LOAD]!!.divisions.asSequence()
            .flatMap { it.groups }.flatMap { it.teams }.filter { it.team == "Atlas 7320" }
            .flatMap { it.people }.map { person: Person -> "${person.lastName}, ${person.firstName}" }.toSet()

        assertEquals(25, folksOfAtlas.size)
        assertTrue {
            folksOfAtlas.containsAll(
                setOf(
                    "Ruiz, Betty", "Flores, Maria", "Ramirez, Justin", "Howard, Justin", "Thompson, Brenda", "Sanchez, Jonathan",
                    "Nguyen, Anthony", "Taylor, Samantha", "Torres, Nicholas", "Ortiz, Kimberly", "Phillips, Olivia", "Chavez, Rachel",
                    "Carter, Mark", "Kim, Ashley", "Flores, Ryan", "Parker, James", "Jackson, Maria", "Gutierrez, Samuel", "Kelly, Ryan",
                    "Hughes, Emma", "Johnson, Justin", "Turner, Gregory", "Howard, Larry", "Clark, Nancy", "Bailey, Sharon"
                )
            )
        }
        tLog.info { "The Atlas team is still intact." }
    }
}
