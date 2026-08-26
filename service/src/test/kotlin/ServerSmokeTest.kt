import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.service.data.status.Matrix
import me.riddle.adventure.web.service.data.status.PAR
import kotlin.test.*

class ServerSmokeTest {

    @Test
    fun `test the root endpoints`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/bogus").status)
        assertEquals(HttpStatusCode.OK, client.get("/static").status)
    }

    @Test
    fun `control plane gets the matrix on connect`() = testApplication {
        configure()
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            val frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            val json = frame.readText()

            // Asserted on the raw text, not the decoded object: `type` is a defaulted property,
            // and decoding puts defaults back, so a typed assertion passes even when the field
            // never reached the wire. It didn't, until `encodeDefaults = true`.
            assertContains(json, "\"type\":\"matrix\"")

            val matrix = Json.decodeFromString<Matrix>(json)
            assertEquals("matrix", matrix.type)
            assertEquals(PAR, matrix)
            assertEquals("Par", matrix.columns.first())
            assertEquals(listOf("Level 1", "Level 2", "Leaves"), matrix.rows.map { it.title })
        }
    }

}
