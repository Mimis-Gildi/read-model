import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.service.data.bench.CorporateDivision
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.status.BLANK
import me.riddle.adventure.web.service.data.status.Board
import me.riddle.adventure.web.service.data.status.Matrix
import me.riddle.adventure.web.service.data.status.MatrixRow
import me.riddle.adventure.web.service.data.status.Module
import me.riddle.adventure.web.service.data.status.PAR
import me.riddle.adventure.web.service.data.status.Report
import me.riddle.adventure.web.service.data.status.Rung
import me.riddle.adventure.web.service.data.status.Vocabulary
import kotlin.test.*

class ServerSmokeTest {

    /**
     * [Board] is an object, so a recorded report outlives the test that sent it, and the next test would assert against
     * a board someone else scribbled on. Reset through the real API rather than a test-only back door.
     */
    @BeforeTest
    fun clearTheBoard() = runBlocking { Board.publish(PAR) }

    @Test
    fun `test the root endpoints`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/bogus").status)
        assertEquals(HttpStatusCode.OK, client.get("/static").status)
    }

    /** The next text frame off the socket, as raw JSON -- what actually crossed the wire. */
    private suspend fun DefaultClientWebSocketSession.nextText(): String =
        incoming.receive().let {
            assertIs<Frame.Text>(it)
            it.readText()
        }

    @Test
    fun `control plane gets the vocabulary before the matrix`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            val json = nextText()

            // Raw text again, for the reason the matrix test documents below: `type` is defaulted.
            assertContains(json, "\"type\":\"vocabulary\"")

            val vocabulary = Json.decodeFromString<Vocabulary>(json)
            assertEquals(listOf("smoke", "bench", "load"), vocabulary.datasets.map { it.key })
            assertEquals(listOf("Smoke", "Bench", "Load"), vocabulary.datasets.map { it.label })
            assertEquals(listOf(1_020, 17_568, 195_312), vocabulary.datasets.map { it.nodes })

            // The literals above are the hand-computed figures from Dataset's own KDoc. This walks
            // the four layers off `branching` instead, so the property is checked against the shape
            // rather than against a copy of itself.
            Dataset.entries.zip(vocabulary.datasets).forEach { (dataset, option) ->
                assertEquals(
                    generateSequence(CorporateDivision.DIVISIONS.size) { it * dataset.branching }.take(4).sum(),
                    option.nodes,
                    "${dataset.key} must count every layer of the tree it serves"
                )
            }

            // The four culling depths, not the five matrix rungs: the reveal is a measurement, not a level.
            assertEquals(listOf("Divisions", "Groups", "Teams", "People"), vocabulary.levels)

            // Connect order is part of the contract: what may be asked for, then what was measured.
            assertContains(nextText(), "\"type\":\"matrix\"")
        }
    }

    @Test
    fun `control plane gets the matrix on connect`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            nextText() // the vocabulary frame; asserted in its own test
            val json = nextText()

            // Asserted on the raw text, not the decoded object: `type` is a defaulted property,
            // and decoding puts defaults back, so a typed assertion passes even when the field
            // never reached the wire. It didn't, until `encodeDefaults = true`.
            assertContains(json, "\"type\":\"matrix\"")

            val matrix = Json.decodeFromString<Matrix>(json)
            assertEquals("matrix", matrix.type)
            assertEquals(PAR, matrix)
            assertEquals("Par", matrix.columns.first())

            // One row per dataset per rung -- fifteen. Titles alone no longer identify a row: there are three of
            // every title, and telling them apart is the entire point of keying by dataset.
            assertEquals(
                Dataset.entries.flatMap { dataset -> Rung.entries.map { dataset.key to it.label } },
                matrix.rows.map { it.dataset to it.title },
            )
            assertEquals(15, matrix.rows.size)
        }
    }

    /** A fixture's report for one rung, as it goes on the wire. */
    private fun report(
        module: String,
        rung: String,
        built: Double,
        painted: Double,
        dataset: String = Dataset.BENCH.key,
    ) = Report(
        run = "test-run", module = module, dataset = dataset,
        rung = rung, elements = 89_436, built = built, painted = painted,
    )

    /** The one row a cell address names. Keyed, never counted -- the same way [Matrix] finds it. */
    private fun Matrix.row(dataset: Dataset, rung: Rung): MatrixRow =
        rows.single { it.dataset == dataset.key && it.title == rung.label }

    @Test
    fun `a report from a fixture lands in that module's column and is broadcast back`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            nextText() // vocabulary
            nextText() // matrix, still par

            send(Json.encodeToString(report("vanilla", Rung.PEOPLE.label, 175.3, 1_413.9)))
            val matrix = Json.decodeFromString<Matrix>(nextText())

            // Formatted server-side, per the matrix's own doctrine: the fixture sent two doubles.
            assertEquals("175.3 / 1413.9", matrix.row(Dataset.BENCH, Rung.PEOPLE).cells[Module.VANILLA.column])

            // ...and nothing else moved. A report is one cell, not a new matrix.
            assertEquals(BLANK, matrix.row(Dataset.BENCH, Rung.PEOPLE).cells[Module.REACT.column])
            assertEquals(PAR.rows - PAR.row(Dataset.BENCH, Rung.PEOPLE), matrix.rows - matrix.row(Dataset.BENCH, Rung.PEOPLE))
        }
    }

    @Test
    fun `an unaddressable report changes nothing and leaves the socket open`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            nextText()
            nextText()

            // A module the matrix has no column for, a rung and a dataset it has no row for, and something that is
            // not a report at all. None of the four may close the channel a run is being recorded over.
            send(Json.encodeToString(report("jquery", Rung.PEOPLE.label, 1.0, 2.0)))
            send(Json.encodeToString(report("vanilla", "Peons", 1.0, 2.0)))
            send(Json.encodeToString(report("vanilla", Rung.PEOPLE.label, 1.0, 2.0, dataset = "torture")))
            send("not json")

            // Proof of life, and proof the board is untouched: a good report still gets through.
            send(Json.encodeToString(report("react", Rung.DIVISIONS.label, 0.4, 8.1)))
            val matrix = Json.decodeFromString<Matrix>(nextText())

            assertEquals("0.4 / 8.1", matrix.row(Dataset.BENCH, Rung.DIVISIONS).cells[Module.REACT.column])
            assertEquals(PAR.row(Dataset.BENCH, Rung.PEOPLE), matrix.row(Dataset.BENCH, Rung.PEOPLE))
        }
    }

    @Test
    fun `the same rung under two datasets lands in two cells`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            nextText() // vocabulary
            nextText() // matrix, still par

            // Identical module, identical level. Before the rows were keyed by dataset these were the same cell, and
            // the second run silently erased the first. This is the regression the whole change exists to prevent.
            send(Json.encodeToString(report("vanilla", Rung.PEOPLE.label, 175.3, 1_413.9, Dataset.BENCH.key)))
            nextText()
            send(Json.encodeToString(report("vanilla", Rung.PEOPLE.label, 1950.2, 3_755.6, Dataset.LOAD.key)))
            val matrix = Json.decodeFromString<Matrix>(nextText())

            assertEquals("175.3 / 1413.9", matrix.row(Dataset.BENCH, Rung.PEOPLE).cells[Module.VANILLA.column])
            assertEquals("1950.2 / 3755.6", matrix.row(Dataset.LOAD, Rung.PEOPLE).cells[Module.VANILLA.column])
            assertEquals(BLANK, matrix.row(Dataset.SMOKE, Rung.PEOPLE).cells[Module.VANILLA.column])
        }
    }

    @Test
    fun `the reveal is its own rung and does not touch the row the ladder built`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            nextText() // vocabulary
            nextText() // matrix, still par

            // The ladder's People rung: 187,500 rows constructed folded, from nothing.
            send(Json.encodeToString(report("vanilla", Rung.PEOPLE.label, 175.3, 1_413.9, Dataset.LOAD.key)))
            nextText()

            // Then the reveal, unfolding what that built. Same dataset, same module, a different measurement --
            // and it reports repeatedly, each chunk carrying the accumulation so far, so the cell is overwritten
            // in place until the fixture dies. The last one that arrived is the answer.
            send(Json.encodeToString(report("vanilla", Rung.REVEAL.label, 12.0, 900.0, Dataset.LOAD.key)))
            nextText()
            send(Json.encodeToString(report("vanilla", Rung.REVEAL.label, 24.5, 2_310.7, Dataset.LOAD.key)))
            val matrix = Json.decodeFromString<Matrix>(nextText())

            assertEquals("24.5 / 2310.7", matrix.row(Dataset.LOAD, Rung.REVEAL).cells[Module.VANILLA.column])
            assertEquals("175.3 / 1413.9", matrix.row(Dataset.LOAD, Rung.PEOPLE).cells[Module.VANILLA.column])
        }
    }

}
