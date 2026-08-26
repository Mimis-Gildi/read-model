package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.service.data.bench.Company
import me.riddle.adventure.web.service.data.bench.CorporateDivision
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Dataset.BENCH
import me.riddle.adventure.web.service.data.bench.Dataset.SMOKE
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The read model over HTTP. `configure()` loads the real `application.yaml`,
 * so every module the server runs is wired here fully, and nothing binds a port,
 * whilst -- production server keeps 48080 for our own amusement.
 */
class DataRoutesTest {

    companion object {

        val tLog = KotlinLogging.logger {}

        /** Divisions, groups, teams, people -- one layer per level, so the count is the level count. */
        const val LEVELS = 4

        val json = Json { ignoreUnknownKeys = true }

        /** A full tree by layer: `12`, `12b`, `12b^2`, `12b^3`, straight off the branching factor. */
        val Dataset.layers: List<Int>
            get() = generateSequence(CorporateDivision.DIVISIONS.size) { it * branching }.take(LEVELS).toList()

        /** What [layers] becomes once the tree is culled to [level]: everything deeper is gone. */
        fun Dataset.layersAt(level: Int): List<Int> =
            layers.mapIndexed { index, count -> if (index <= level) count else 0 }

        /** The same four numbers, counted off a served tree rather than derived from `branching`. */
        fun Company.census(): List<Int> = listOf(
            divisions.size,
            divisions.sumOf { it.groups.size },
            divisions.flatMap { it.groups }.sumOf { it.teams.size },
            divisions.flatMap { it.groups }.flatMap { it.teams }.sumOf { it.people.size },
        )

        /** Every team in document order, identity only -- what culling must leave untouched. */
        fun Company.teamRoll(): List<Pair<Int, String>> =
            divisions.flatMap { it.groups }.flatMap { it.teams }.map { it.id to it.team }
    }

    private suspend fun ApplicationTestBuilder.company(path: String): Company =
        client.get(path).let {
            assertEquals(HttpStatusCode.OK, it.status, "GET $path")
            json.decodeFromString(it.bodyAsText())
        }

    @Test
    fun `the bare route serves the whole tree`() = testApplication {
        configure()
        Dataset.entries.forEach { dataset ->
            assertEquals(
                dataset.layers,
                company("/data/${dataset.key}").census(),
                "GET /data/${dataset.key} must serve every layer in full"
            )
            tLog.info { "/data/${dataset.key} served ${dataset.layers} " }
        }
    }

    @Test
    fun `every level culls the layers below it`() = testApplication {
        configure()
        Dataset.entries
            .flatMap { dataset -> (0 until LEVELS).map { dataset to it } }
            .forEach { (dataset, level) ->
                assertEquals(
                    dataset.layersAt(level),
                    company("/data/${dataset.key}/$level").census(),
                    "GET /data/${dataset.key}/$level must cull everything deeper than $level"
                )
                tLog.info { "/data/${dataset.key}/$level served ${dataset.layersAt(level)}" }
            }
    }

    @Test
    fun `the deepest level is the untouched tree`() = testApplication {
        configure()
        // SMOKE and BENCH only: LOAD is 25 MB a copy, and four of those in flight buys nothing the
        // census assertions above do not already cover.
        listOf(SMOKE, BENCH).forEach { dataset ->
            assertEquals(
                client.get("/data/${dataset.key}").bodyAsText(),
                client.get("/data/${dataset.key}/${LEVELS - 1}").bodyAsText(),
                "level ${LEVELS - 1} must be byte-identical to the bare route for ${dataset.key}"
            )
        }
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

    @Test
    fun `unknown datasets and levels are not found`() = testApplication {
        configure()
        listOf(
            "/data/nope",
            "/data/nope/0",
            "/data/Smoke",
            "/data/smoke/",
            "/data/smoke/-1",
            "/data/smoke/$LEVELS",
            "/data/smoke/x",
        ).forEach {
            assertEquals(HttpStatusCode.NotFound, client.get(it).status, "GET $it must not be found")
        }
    }
}
