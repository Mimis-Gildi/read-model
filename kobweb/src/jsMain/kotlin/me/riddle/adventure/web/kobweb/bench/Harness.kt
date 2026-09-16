/*
 * The contract is the shared harness, `harness/Harness.kt`. Where this file and that one disagree, this file is the bug.
 *
 *   - fetch, JSON parse, census and teardown are OUTSIDE the clock;
 *   - `built` is stamped the line after the `attach` returns -- construction, no layout, no paint;
 *   - `painted` is the frame after that, taken with a double `requestAnimationFrame`;
 *   - a rung is posted the moment it completes, so a run that dies still says how far it got;
 *   - a tab that goes dark mid-measurement spoils the reading, and the rung is re-run rather than reported.
 *
 * It is a port and not a reuse because the shared harness owns the DOM it measures -- it clears the host, reads cells back
 * out of the table and folds through `querySelectorAll`. Compose owns that DOM here, and two writers on one tree is
 * worse than the duplication.
 *
 * CAUTION on `attach`: it mounts a fresh `renderComposable` root per rung, disposed by the next reset. Compose commits
 * an initial composition synchronously, which is the only reason `built` is measurable at all. A state change into a
 * standing composition is scheduled through the Recomposer and the line after it would stamp nothing -- which is also
 * why the reveal folds through the DOM rather than through state.
 */
package me.riddle.adventure.web.kobweb.bench

import androidx.compose.runtime.Composition
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import me.riddle.adventure.web.model.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.Element
import org.w3c.dom.WebSocket
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.url.URLSearchParams
import kotlin.coroutines.resume
import kotlin.js.json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * [REVEAL] is not a level -- there is no `/data/<dataset>/4`. It is People finally being laid out.
 * FixMe: Slop - rip out.
 * */
enum class Rung(val id: String, val title: String) {
    DIVISIONS("level-0", "Divisions"),
    GROUPS("level-1", "Groups"),
    TEAMS("level-2", "Teams"),
    PEOPLE("level-3", "People"),
    REVEAL("reveal", "Reveal");

    val depth: Int get() = ordinal

    companion object {
        val levels = entries - REVEAL
    }
}

class Reading(val elements: Int, val built: Double, val painted: Double)

/**
 * Kobweb serves this page on its own port, so unlike the other three fixtures the service is not `location.host`.
 *
 * [step] sits just below the boundary React survives; [pause] lets the browser off the layout-thrashing cliff.
 */
class Launch(
    val run: String,
    val dataset: String,
    val eventChannel: String,
    val step: Int,
    val pause: Int,
) {
    @Suppress("HttpUrlsUsage")
    val http get() = "http://$eventChannel"
    val ws get() = "ws://$eventChannel"

    companion object {
        fun read(): Launch = URLSearchParams(window.location.search).let { params ->
            Launch(
                run = params.get(PARAMETER_RUN_ID).orEmpty().ifEmpty { TimeSource.Monotonic.markNow().toString() },
                dataset = params.get(PARAMETER_DATASET).orEmpty().ifEmpty { DEFAULT_VALUE_DATASET },
                eventChannel = params.get(PARAMETER_EVENT_CHANNEL).orEmpty().ifEmpty { window.location.host },
                step = params.get(PARAMETER_STEP_SIZE)?.toIntOrNull() ?: DEFAULT_VALUE_STEP_SIZE,
                pause = params.get(PARAMETER_THREAD_RECOVERY_PAUSE_MS)?.toIntOrNull() ?: DEFAULT_VALUE_THREAD_RECOVERY_PAUSE_MS,
            )
        }
    }
}

fun ms(n: Double): String = "${n.asDynamic().toFixed(1) as String} ms"

/** Kotlin's DOM externals do not carry the Page Visibility flag. */
private val hidden: Boolean get() = document.asDynamic().hidden as Boolean

private class Fold(val box: Element, val rows: Int)

class Harness(val launch: Launch, private val host: Element) {

    private val mounted = mutableListOf<Composition>()
    private var darkened = false
    private var teardown = 0.0
    private var nodesOnScreen = 0
    private val socket = WebSocket("${launch.ws}/ws")

    init {
        document.addEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE, { darkened = darkened || hidden })
        host.addEventListener(ON_CLICK, { event ->
            foldable(event)?.let { box -> shut(box, !box.folded()) }
        })
    }

    private fun now() = window.performance.now()

    private suspend fun nextPaint(): Double = suspendCancellableCoroutine { continued ->
        window.requestAnimationFrame { window.requestAnimationFrame { continued.resume(now()) } }
    }

    /** Chrome does not run `requestAnimationFrame` in a hidden tab causing [nextPaint] to never settle. */
    private suspend fun onScreen(): Unit = if (!hidden) Unit else suspendCancellableCoroutine { continued ->
        document.addEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE, object : EventListener {
            override fun handleEvent(event: Event) = if (hidden) Unit else {
                document.removeEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE, this)
                continued.resume(Unit)
            }
        })
    }

    /** Await once, outside every clock because connection is lazy. */
    private suspend fun connected(): Boolean = when (socket.readyState) {
        WebSocket.OPEN -> true
        WebSocket.CONNECTING -> suspendCancellableCoroutine { waiting ->
            var settled = false
            val settle = { open: Boolean -> if (!settled) settled = true.also { waiting.resume(open) } else Unit }
            socket.onopen = { settle(true) }
            socket.onerror = { settle(false) }
            socket.onclose = { settle(false) }
        }

        else -> false
    }

    private suspend fun fetchLevel(level: Int): dynamic = window
        .fetch("${launch.http}/data/${launch.dataset}/$level").await().let { response ->
            if (response.ok) response.json().await()
            else throw IllegalStateException("${response.status} for ${launch.dataset}/$level")
        }

    /** Outside every clock, so a level is never charged for tearing down the one before it. */
    private fun reset() {
        mounted.onEach(Composition::dispose).clear()
        host.asDynamic().replaceChildren()
    }

    private fun attach(company: dynamic) {
        mounted += renderComposable(root = host) { Tree(company) }
    }

    /** [elements] is passed in rather than returned because the census is taken outside the clock. */
    private suspend fun measure(company: dynamic, elements: Int, status: (String) -> Unit): Reading {
        // FixMe: refactor this imperative slop
        onScreen()
        darkened = false

        val cleared = now()
        reset()
        teardown += now() - cleared

        val started = now()
        attach(company)
        val built = now()

        val painted = nextPaint()

        return if (!darkened) Reading(elements, built - started, painted - started)
        else {
            status("BOOM -- tab went dark mid-render -- discarded, re-running.")
            measure(company, elements, status)
        }
    }

    private suspend fun post(rung: Rung, reading: Reading) {
        // FixMe: Refactor this imperative slop
        if (!connected()) return
        socket.send(
            JSON.stringify(
                json(
                    "type" to "report", PARAMETER_RUN_ID to launch.run, PARAMETER_UI_FRAMEWORK to UI_FRAMEWORK_KOBWEB.first, PARAMETER_DATASET to launch.dataset,
                    "rung" to rung.title,
                    "elements" to reading.elements, "built" to reading.built, "painted" to reading.painted,
                )
            )
        )
    }

    /** [fill] receives the model node count alongside the reading, because the table shows both. */
    suspend fun ladder(status: (String) -> Unit, fill: (Rung, Int, Reading) -> Unit) {
        // FixMe: imperative slop
        teardown = 0.0

        if (hidden) {
            status("Waiting: bring this tab to the front -- paint cannot be measured in a background tab.")
        }

        Rung.levels.forEach { rung ->
            status("Level ${rung.depth}: fetching…")
            val company = fetchLevel(rung.depth)

            status("Level ${rung.depth}: rendering…")
            val census = census(company)
            nodesOnScreen = census.nodes
            val reading = measure(company, census.elements, status)

            fill(rung, census.nodes, reading)
            post(rung, reading)
        }

        status("Ladder complete. ${ms(teardown)} of teardown, billed to no rung.")
    }

    suspend fun collapseAll(status: (String) -> Unit) {
        val started = now()
        host.querySelectorAll(".kids").asList().forEach { kids -> shut(kids.parentElement.unsafeCast<Element>(), true) }
        val toggled = now()
        val painted = nextPaint()

        status("Collapsed: ${ms(toggled - started)} toggling, ${ms(painted - started)} to paint")
    }

    private fun chunked(): List<List<Fold>> = host.querySelectorAll(".node.collapsed").asList().map { node -> node.unsafeCast<Element>() }.map { box -> Fold(box, box.querySelector(":scope > .kids")?.childElementCount ?: 0) }
        .fold(mutableListOf<MutableList<Fold>>()) { chunks, fold ->
            chunks.apply {
                if (isEmpty() || last().sumOf(Fold::rows) >= launch.step) add(mutableListOf())
                last().add(fold)
            }
        }

    /**
     * The reveal reports many times against the same row, each chunk carrying the accumulation so far, so the cell
     * holds the last chunk that survived. Chunking is what keeps the run off the layout-thrashing cliff.
     */
    suspend fun reveal(status: (String) -> Unit, fill: (Rung, Int, Reading) -> Unit) {
        val chunks = chunked()
        val hiddenRows = chunks.sumOf { chunk -> chunk.sumOf(Fold::rows) }

        var revealed = 0
        var built = 0.0
        var painted = 0.0
        var remaining = chunks.sumOf { chunk -> chunk.size }

        for (chunk in chunks) {
            onScreen()
            darkened = false

            val started = now()
            chunk.forEach { fold -> shut(fold.box, false) }
            val toggled = now()
            val stamp = nextPaint()

            if (darkened) {
                status("Reveal: BOOM -- tab went dark after ${count(revealed)} rows -- accumulation abandoned.")
                return
            }

            revealed += chunk.sumOf(Fold::rows)
            remaining -= chunk.size
            built += toggled - started
            painted += stamp - started

            val reading = Reading(revealed, built, painted)
            fill(Rung.REVEAL, nodesOnScreen, reading)
            post(Rung.REVEAL, reading)

            status(
                "Reveal: ${count(revealed)} rows, ${ms(painted)} to paint" + if (remaining > 0) " -- ${count(remaining)} teams folded, " + "${count(hiddenRows - revealed)} people still hidden…" else ""
            )

            delay(launch.pause.toLong().milliseconds)
        }

        status("Reveal complete: ${count(revealed)} rows, ${ms(painted)} to paint.")
    }
}
