import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerSmokeTest {

    @Test
    fun `test the root endpoints`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/bogus").status)
        assertEquals(HttpStatusCode.OK, client.get("/static").status)
    }

}
