import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.service.data.bench.CorporateDivision
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.status.Matrix
import me.riddle.adventure.web.service.data.status.PAR
import me.riddle.adventure.web.service.data.status.Vocabulary
import kotlin.test.*

class ServerSmokeTest {

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
            assertEquals(listOf("Divisions", "Groups", "Teams", "People"), matrix.rows.map { it.title })
        }
    }

}
