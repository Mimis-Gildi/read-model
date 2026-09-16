/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
@file:JsExport

package me.riddle.adventure.web.harness

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.*
import me.riddle.adventure.web.model.bench.Company
import me.riddle.adventure.web.model.status.Report
import org.w3c.dom.*
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event
import org.w3c.dom.url.URLSearchParams
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The measurement harness. Shared, verbatim, by every framework under test.
 *
 * These are only TWO things:
 *
 * 1. Contract we use in measuring materialized by the fixture.
 * 2. Commands a fixture expects to receive and process.
 *
 */


/**
 * For what the framework fixture supplies; see [start].
 *
 * The fixture uses this DSL to run measurements consistently across frameworks.
 */
external interface Fixture {

    /** Which column of the matrix this fixture's reports belong in. Hardcoded by the fixture!! */
    val uiFramework: String

    /**
     * Builds the culled tree and mounts it, measuring DOM construction (build).
     * `Person` ships folded at the last level.
     * Returns the number of elements it created.
     */
    fun build(company: Company): Int

    /** Tears the previous rung down outside any clock: same DOM. React will uniquely crash here also. */
    fun reset()

    /** Unfolds the next chunk of up to [count] folded `Person` nodes. Returns how many it actually unfolded. */
    fun unfold(count: Int): Int

    /** Folds the next chunk of up to [count] folded `Person` nodes. Returns how many it actually folded. */
    fun fold(count: Int): Int
}

/** One BUILD measured render (a ladder rung: build outside any clock, then paint). */
data class Measurement(val elements: Int, val built: Double, val painted: Double)

/** One REVEAL measured chunk of the reveal: no build, just toggling classes on already-built DOM, then paint. */
data class RevealMeasurement(val rows: Int, val toggled: Double, val painted: Double)

private external interface Performance {
    fun now(): Double
}

/** The global clock; there is no typed `window.performance` to lean on across browser bindings. */
private external val performance: Performance

// @formatter:off
private val params                      = URLSearchParams(window.location.search)

private val dataset                     = params.get(PARAMETER_DATASET).orEmpty().ifEmpty {DEFAULT_VALUE_DATASET}
private val runId                       = params.get(PARAMETER_RUN_ID).orEmpty().ifEmpty {TimeSource.Monotonic.markNow().toString()}
private val threadRecoveryPauseMs       = params.get(PARAMETER_THREAD_RECOVERY_PAUSE_MS)?.toIntOrNull() ?: DEFAULT_VALUE_THREAD_RECOVERY_PAUSE_MS
private val revealStepSize              by lazy { params.get(PARAMETER_STEP_SIZE)?.toIntOrNull() ?: DEFAULT_VALUE_STEP_SIZE }

private fun fixtureElement(id: String)  = document.getElementById(id) as HTMLElement
private val fixtureTree                 by lazy { fixtureElement(DOM_KEY_TREE_ROOT) }
private val fixtureRun                  by lazy { fixtureElement(DOM_KEY_RUN) }
private val fixtureStatus               by lazy { fixtureElement(DOM_KEY_STATUS) }
private val fixtureRows                 by lazy { fixtureElement("rows") }
private val fixtureConnection           by lazy { fixtureElement(DOM_KEY_CONNECTION) }
private val fixtureConnectionText       by lazy { fixtureElement(DOM_KEY_CONNECTION_TEXT) }
// @formatter:on

/** The socket's state, said out loud. Same [HTML5_DATA_STATE] contract the control plane's stylesheet reads. */
private fun setConnectionStatusFixture(status: ConnectionStatus) = fixtureConnection
    .apply { setAttribute(HTML5_DATA_STATE, status.dataState) }
    .also { fixtureConnectionText.textContent = status.label }


/** Two escapes into JS number formatting: the platform has no Kotlin equivalent of either. */
private fun Int.grouped(): String = asDynamic().toLocaleString() as String
private fun Double.ms(): String = "${asDynamic().toFixed(1) as String} ms"


/** The bindings carry no indexed access on a live collection, and no `hidden` on the document. */
private fun NodeList.elements(): List<Element> = (0 until length).mapNotNull { item(it) as? Element }
private fun HTMLTableRowElement.cell(at: Int) = cells.item(at) as? HTMLElement

private val hidden: Boolean get() = document.asDynamic().hidden as Boolean

private val scope = CoroutineScope(Dispatchers.Main)

/** The meat: framework fixture under test, handed over by [start]. */
private lateinit var fixture: Fixture


/** The pause between reveal chunks. Deliberately outside every clock to allow the browser to recover off of the Cliff. */
private suspend fun breathe() = delay(threadRecoveryPauseMs.milliseconds)

/** Resolves on the frame after the one the caller's DOM work completes (once it is painted). */
@Suppress("NON_EXPORTABLE_TYPE")
private suspend fun nextPaint(): Double = suspendCancellableCoroutine { waiting ->
    window.requestAnimationFrame {
        window.requestAnimationFrame { waiting.resume(performance.now()) }
    }
}

/** Set on a tab-hidden event so a paused run can be recognized and re-measured instead of counted as valid. */
private var darkened = false

/** Resolves once the tab is actually on screen. An opportunity to inject some recovery code. */
@Suppress("NON_EXPORTABLE_TYPE")
private suspend fun onScreen() = when {
    !hidden -> Unit
    else -> suspendCancellableCoroutine { waiting ->
        lateinit var seen: (Event) -> Unit
        seen = {
            when {
                hidden -> Unit
                else -> document.removeEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE, seen).also { waiting.resume(Unit) }
            }
        }
        document.addEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE, seen)
    }
}

/** The tree culled to [level] outside the clock because transport is not part of this experiment. */
private suspend fun fetchLevel(level: Int): Company = window.fetch("/data/$dataset/$level", js("({})")).await().let { response ->
    when {
        response.ok -> Json.decodeFromString(Company.serializer(), response.text().await())
        else -> throw IllegalStateException("${response.status} for $dataset/$level")
    }
}

/**
 * One rung:
 * 1. Build the culled tree.
 * 2. Attach it in a single operation.
 * 3. And, timestamp twice.
 *
 * Teardown of the previous rung happens before `started` -- this level is never charged for the DOM
 * the level before it left behind.
 *
 * A tab hidden mid-measurement stops painting, which invalidates the result; on that event the rung is
 * discarded and re-measured against the same already-fetched `company`.
 */
private suspend fun measure(company: Company, level: Int): Measurement {
    onScreen()
    darkened = false

    val cleared = performance.now()
    fixture.reset()
    teardown += performance.now() - cleared

    val started = performance.now()
    val elements = fixture.build(company)
    val built = performance.now()

    val painted = nextPaint()

    return when {
        !darkened -> Measurement(elements, built - started, painted - started)
        else -> {
            fixtureStatus.textContent = "Level $level: BOOM -- tab went dark mid-render -- discarded, re-running."
            measure(company, level)
        }
    }
}

/** Count model objects on the served tree instead of the service-derived value trusted. */
private fun census(company: Company): Int = company.divisions.sumOf { division ->
    1 + division.groups.sumOf { group ->
        1 + group.teams.sumOf { team -> 1 + team.people.size }
    }
}

/**
 * The "rung" in the result row. These five "level" names materialize in the fixture's HTML exactly once, and in the
 * first cell of each row, and they are the same strings the service files a report under. Reading them back for fidelity.
 */
private fun rungOf(row: HTMLTableRowElement) = row.cell(0)?.textContent.orEmpty()

private fun fill(row: HTMLTableRowElement, nodes: Int, result: Measurement) =
    listOf(nodes.grouped(), result.elements.grouped(), result.built.ms(), result.painted.ms()).forEachIndexed { column, value -> row.cell(column + 1)?.textContent = value }

private fun fillReveal(row: HTMLTableRowElement, result: RevealMeasurement) = listOf(result.rows.grouped(), result.toggled.ms(), result.painted.ms()).forEachIndexed { column, value -> row.cell(column + 1)?.textContent = value }

/**
 * The ladder's rungs are read off the Ktor (service) result matrix in the depth order.
 */
private fun ladderRows() = fixtureRows.querySelectorAll("tr[id^=\"level-\"]").elements().filterIsInstance<HTMLTableRowElement>()

/**
 * The socket to the control plane. Opened once on `load` so that no rung ever pays for a handshake.
 *
 * The fixture sends measurements and ignores everything coming the other way because the matrix is for
 * the control plane to render, not for the thing that's being measured.
 *
 * Reporting per rung rather than per run is the whole point: a fixture that dies on LOAD, the dataset designed to crash
 * a wanting framework like React, never sends a summary because of the expected crash, so the last rung the server heard
 * about is the failure point. That only works if the report leaves before the next, larger rung is attempted.
 */
private val socket = WebSocket("ws://${window.location.host}/ws")

/** Post a finished rung at the time no measurement is going on. */
private fun post(row: HTMLTableRowElement, result: Measurement) = when (socket.readyState) {
    WebSocket.OPEN -> socket.send(
        Json.encodeToString(
            Report(
                runId = runId,
                uiFramework = fixture.uiFramework,
                datasetKey = dataset,
                rung = rungOf(row),
                elements = result.elements,
                built = result.built,
                painted = result.painted,
            )
        )
    )

    else -> dropped(row)
}

/**
 * A rung that never reached the control plane. The indicator is the honest record of it: the run keeps measuring, but
 * the matrix on the other side is missing this row and the page says so rather than letting it read as complete.
 */
private fun dropped(row: HTMLTableRowElement) = setConnectionStatusFixture(ConnectionStatus.OFFLINE)
    .also { console.error("Dropped ${rungOf(row)} report: socket ${socket.readyState}") }

/** Post a finished reveal chunk. `Report`'s wire shape is unchanged; `rows` rides in as `elements`. */
private fun postReveal(row: HTMLTableRowElement, result: RevealMeasurement) = when (socket.readyState) {
    WebSocket.OPEN -> socket.send(
        Json.encodeToString(
            Report(
                runId = runId,
                uiFramework = fixture.uiFramework,
                datasetKey = dataset,
                rung = rungOf(row),
                elements = result.rows,
                built = result.toggled,
                painted = result.painted,
            )
        )
    )

    else -> dropped(row)
}

private fun buttons() = listOf(COMMAND_BUILD_DOM, COMMAND_EXPAND_TEAMS, COMMAND_COLLAPSE_TEAMS).map { fixtureElement(it) as HTMLButtonElement }

/** Model nodes of whatever the ladder last put on the page for reveal to lay these out. */
private var nodesOnScreen = 0

/** Teardown the ladder: the time [measure] spent in `fixture.reset`; i.e., React core weakness. */
private var teardown = 0.0

/** The ladder: level 0 through 3, each fetched then measured, reported as it completes. */
private suspend fun ladder() {
    buttons().forEach { it.disabled = true }
    fixtureRows.querySelectorAll("td:not(:first-child)").elements().forEach { it.textContent = "-" }
    teardown = 0.0

    if (hidden) fixtureStatus.textContent = "Waiting: bring this tab to the front -- paint cannot be measured in a background tab."

    ladderRows().forEachIndexed { level, row ->
        fixtureStatus.textContent = "Level $level: fetching…"
        val company = fetchLevel(level)
        fixtureStatus.textContent = "Level $level: rendering…"
        val result = measure(company, level)
        nodesOnScreen = census(company)
        fill(row, nodesOnScreen, result)
        post(row, result)
    }

    fixtureStatus.textContent = "Ladder complete. ${teardown.ms()} of teardown, billed to no rung."
    buttons().forEach { it.disabled = false }
}

/**
 * Toggles in survivable chunks until [verb] has nothing left, handing [each] the accumulation so far.
 * Returns the whole accumulation, or null when the tab went dark and spoiled it.
 * An empty chunk is not a chunk: it is neither painted, nor accumulated, nor reported.
 */
private suspend fun chunked(verb: (Int) -> Int, each: (RevealMeasurement) -> Unit): RevealMeasurement? {
    var total = RevealMeasurement(0, 0.0, 0.0)
    while (true) {
        onScreen()
        darkened = false

        val started = performance.now()
        val rows = verb(revealStepSize)
        val stamp = performance.now()
        if (rows == 0) return total

        val paint = nextPaint()
        if (darkened) return null

        total = RevealMeasurement(total.rows + rows, total.toggled + stamp - started, total.painted + paint - started)
        each(total)
        breathe()
    }
}

/** Runs [act] with every button disabled, whatever way it ends. */
private suspend fun exclusively(act: suspend () -> Unit) {
    buttons().forEach { it.disabled = true }
    try {
        act()
    } finally {
        buttons().forEach { it.disabled = false }
    }
}

/** Fold the teams back shut in survivable chunks: exactly what the reveal opened, and nothing above it. Not reported. */
private suspend fun collapseTeams() = exclusively {
    fixtureStatus.textContent = chunked(fixture::fold) { fixtureStatus.textContent = "Collapse: ${it.rows.grouped()} rows, ${it.painted.ms()} to paint" }
        ?.let { "Collapse complete: ${it.rows.grouped()} rows, ${it.toggled.ms()} toggling, ${it.painted.ms()} to paint." }
        ?: "Collapse: BOOM -- tab went dark mid-fold -- accumulation abandoned."
}

/** The reveal: unfold the whole tree in survivable chunks, measuring till finish or the browser crash. */
private suspend fun reveal() = exclusively {
    val row = fixtureElement("reveal") as HTMLTableRowElement
    fixtureStatus.textContent = chunked(fixture::unfold) {
        fillReveal(row, it)
        postReveal(row, it)
        fixtureStatus.textContent = "Reveal: ${it.rows.grouped()} rows, ${it.painted.ms()} to paint"
    }
        ?.let { "Reveal complete: ${it.rows.grouped()} rows, ${it.painted.ms()} to paint." }
        ?: "Reveal: BOOM -- tab went dark mid-reveal -- accumulation abandoned."
}

/**
 * Hand the harness a framework fixture to run. The only entry point.
 *
 * Every framework fixture is driven by identical code.
 */
fun start(frameworkFixture: Fixture) {
    fixture = frameworkFixture

    setConnectionStatusFixture(ConnectionStatus.CONNECTING)
    listOf<Pair<String, (Event) -> Unit>>(
        ON_OPEN to { setConnectionStatusFixture(ConnectionStatus.LIVE) },
        ON_CLOSE to { setConnectionStatusFixture(ConnectionStatus.OFFLINE) },
        ON_ERROR to { setConnectionStatusFixture(ConnectionStatus.OFFLINE) },
    ).fold(socket) { open, (event, handler) -> open.apply { addEventListener(event, handler) } }

    document.addEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE) { darkened = darkened || hidden }

    fixtureElement(COMMAND_BUILD_DOM).addEventListener(ON_CLICK, { scope.launch { ladder() } })
    fixtureElement(COMMAND_EXPAND_TEAMS).addEventListener(ON_CLICK, { scope.launch { reveal() } })
    fixtureElement(COMMAND_COLLAPSE_TEAMS).addEventListener(ON_CLICK, { scope.launch { collapseTeams() } })

    fixtureRun.textContent = runId.ifEmpty { "-" }
    fixtureElement("datasetKey").textContent = dataset.ifEmpty { "-" }
    (fixtureElement(COMMAND_BUILD_DOM) as HTMLButtonElement).disabled = dataset.isEmpty()
}

/** The container everything renders into. */
@Suppress("unused")
val host: Element get() = fixtureTree
