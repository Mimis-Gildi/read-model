package me.riddle.adventure.web.service.module

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Level
import me.riddle.adventure.web.service.data.service.HumanResourcesDataService
import me.riddle.adventure.web.service.data.status.PerformanceScoreTable
import me.riddle.adventure.web.service.data.status.Module
import me.riddle.adventure.web.service.data.status.Report

private val logger = KotlinLogging.logger {}

/** One instance for the whole application; it generates on the first request to memoize thereafter. */
private val humanResources = HumanResourcesDataService()

fun Application.configureRouting() {

    /**
     * Routing configuration for status WebSocket and HTTP data endpoints.
     */
    routing {

        /**
         * Single socket for two kinds of clients:
         * - Control planes, any count, listen to show statistics, and issue commands;
         * - And fixtures that execute benchmarks and send a "Report" per measured rung.
         *
         * Typed for basic competence. An unreadable frame is logged and skipped rather.
         * No errors are thrown on WebSocket communications because these are only events and commands.
         * A fixture with a typo in its payload must not be able to drop or even influence a channel.
         */
        webSocket("/ws") {
            PerformanceScoreTable.join(this)
            try {
                for (frame in incoming) {
                    logger.debug { "Received frame: $frame" }
                    when (frame) {
                        is Frame.Text -> frame.readText().let { text ->
                            runCatching { Json.decodeFromString<Report>(text) }
                                .onSuccess { report ->
                                    PerformanceScoreTable.record(report)
                                    logger.debug { "Recorded report for ${report.module}->${report.dataset}->${report.rung} (${report.run})" }
                                }
                                .onFailure { error ->
                                    logger.warn(error) { "Unreadable frame: $text" }
                                }
                        }

                        else -> logger.warn { "Ignored non-text frame: $frame" }
                    }
                }
            } finally {
                PerformanceScoreTable.leave(this)
            }
        }

        get("/data/{dataset}") { // the whole tree, one dataset per request
            when (val dataset = Dataset.of(call.parameters["dataset"])) {
                null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["dataset"]}")
                else -> call.respond(humanResources.get(dataset))
            }
        }

        get("/data/{dataset}/{level}") { // the same tree culled to a level
            val dataset = Dataset.of(call.parameters["dataset"])
            val level = call.parameters["level"]?.toIntOrNull()?.let(Level::at)
            when {
                dataset == null -> call.respond(HttpStatusCode.NotFound, "No such dataset: ${call.parameters["dataset"]}")
                level == null -> call.respond(HttpStatusCode.NotFound, "No such level: ${call.parameters["level"]}")
                else -> call.respond(humanResources.get(dataset, level))
            }
        }

        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        // One mount per fixture, named for the module that owns it -- so a framework's assets never sit inside
        // another framework's tree, and adding a module is adding a directory rather than nesting in someone else's.
        //
        // The mount path is [Module.key]: the same string the fixture carries on its URL, puts on the wire in a
        // [Report], and is addressed by in the matrix. One string, so the launch URL is derivable and the control
        // plane has nothing to hard-code.
        Module.entries.forEach { staticResources("/${it.key}", it.key) }

        // The measurement harness and the row stylesheet: shared by every fixture above, owned by none of them.
        // Its own mount rather than a copy per module, because a second copy of a stopwatch is a second set of numbers.
        staticResources("/harness", "harness")

        // Third-party runtimes, vendored into the repo rather than fetched at run time -- a benchmark that reaches for
        // a CDN measures the CDN, and a version that can move underneath us is not a result anyone can reproduce.
        //
        // Pinned: react 19.2.8, react-dom 19.2.8 with its client entry, and scheduler 0.27.0 -- esm.sh's es2022
        // builds. React 19 ships no UMD at all, so a script tag has to be a module and the files have to come from
        // somewhere; they come from here. Each carries its own `/* esm.sh - react@19.2.8 */` header, so the pin is on
        // the artifact rather than only on this comment.
        //
        // Four unbundled files rather than react-dom's one self-contained `client.bundle.mjs`, because the fixture
        // needs `flushSync` -- which lives in react-dom proper -- to act on the root `createRoot` returns. Bundled,
        // those are two copies of react-dom's internals and the flush would apply to a root that does not exist.
        // Unbundled, client.mjs imports react-dom.mjs, and there is one instance.
        //
        // Upgrading is re-running the curls and re-pointing each absolute `/react@…`, `/scheduler@…` import at its
        // sibling. There should be none left: `grep 'from"/'` across the directory must come back empty.
        staticResources("/vendor", "vendor")

        staticResources("/", "control")
    }
}
