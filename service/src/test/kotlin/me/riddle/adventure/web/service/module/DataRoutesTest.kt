package me.riddle.adventure.web.service.module

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.bench.Company
import me.riddle.adventure.web.model.bench.Dataset.BENCH
import me.riddle.adventure.web.model.bench.Dataset.SMOKE
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The read model over HTTP. `configure()` loads the real `application.yaml`,
 * so every fixture the server runs is wired here fully, and nothing binds a port,
 * whilst -- production server keeps 48080 for our own amusement.
 */
class DataRoutesTest {

    companion object {


        val json = Json { ignoreUnknownKeys = true }


        fun Company.teamRoll(): List<Pair<Int, String>> =
            divisions.flatMap { it.groups }.flatMap { it.teams }.map { it.id to it.team }
    }

    private suspend fun ApplicationTestBuilder.company(path: String): Company =
        client.get(path).let {
            assertEquals(HttpStatusCode.OK, it.status, "GET $path")
            json.decodeFromString(it.bodyAsText())
        }

    @Test
    fun `culling leaves the surviving layers untouched`() = testApplication {
        configure()
        assertEquals(
            company("/data/${SMOKE.key}").teamRoll(),
            company("/data/${SMOKE.key}/2").teamRoll(),
            "a culled team keeps its id and name -- only its people go"
        )
    }

    @Test
    fun `the same request twice is byte-identical`() = testApplication {
        configure()
        assertEquals(
            client.get("/data/${BENCH.key}/2").bodyAsText(),
            client.get("/data/${BENCH.key}/2").bodyAsText(),
            "the tree is generated once and served, so nothing may vary between requests"
        )
    }

    @Test
    fun `the wire carries the key, not the constant`() = testApplication {
        configure()
        assertEquals(SMOKE, company("/data/${SMOKE.key}").dataset, "the served tree names its own dataset")
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/data/${SMOKE.name}").status,
            "the URL is addressed by ${SMOKE.key}, never by ${SMOKE.name}"
        )
    }

}
