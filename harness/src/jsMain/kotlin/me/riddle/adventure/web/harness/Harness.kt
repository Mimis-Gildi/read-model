/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
@file:JsExport

package me.riddle.adventure.web.harness

import io.github.oshai.kotlinlogging.KotlinLogging
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

    /**
     * Builds the culled tree and mounts it, measuring DOM construction (build).
     * `Person` ships folded at the last level.
     * Returns the number of elements it created.
     */
    fun build(company: Company): Int

    /** Tears one rung down measuring its time and returning the number of `Node`s collapsed. */
    fun collapse(): Int

    /** Tears the previous rung down outside any clock: same DOM. React will uniquely crash here also. */
    fun reset()

    /** Changes global chunk size reporting previous chunk size. Default chunk size remains on URL parameters.  */
    fun setChunkSize(size: Int): Int

    /** Unfolds the next chunk of up to [count] folded `Person` nodes. Returns how many it actually unfolded. */
    fun unfold(count: Int): Int

    /** Folds the next chunk of up to [count] folded `Person` nodes. Returns how many it actually folded. */
    fun fold(count: Int): Int

    /** Folds every currently unfolded `Person` node. Returns how many it folded. */
    fun foldAll(): Int
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
private val uiFramework                 = params.get(PARAMETER_UI_FRAMEWORK).orEmpty().ifEmpty { DEFAULT_UI_FRAMEWORK}
private val threadRecoveryPauseMs       = params.get(PARAMETER_THREAD_RECOVERY_PAUSE_MS)?.toIntOrNull() ?: DEFAULT_VALUE_THREAD_RECOVERY_PAUSE_MS
private val revealStepSize              by lazy { params.get(PARAMETER_STEP_SIZE)?.toIntOrNull() ?: DEFAULT_VALUE_STEP_SIZE }

private fun fixtureElement(id: String)  = document.getElementById(id) as HTMLElement
private val fixtureTree                 by lazy { fixtureElement(DOM_KEY_TREE_ROOT) }
private val fixtureRun                  by lazy { fixtureElement(DOM_KEY_RUN) }
private val fixtureStatus               by lazy { fixtureElement(DOM_KEY_STATUS) }
private val fixtureRows                 by lazy { fixtureElement("rows") }
// @formatter:on


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
                uiFramework = uiFramework,
                datasetKey = dataset,
                rung = rungOf(row),
                elements = result.elements,
                built = result.built,
                painted = result.painted,
            )
        )
    )

    else -> {
        console.error("Dropped ${rungOf(row)} report: socket ${socket.readyState}")
    }
}

/** Post a finished reveal chunk. `Report`'s wire shape is unchanged; `rows` rides in as `elements`. */
private fun postReveal(row: HTMLTableRowElement, result: RevealMeasurement) = when (socket.readyState) {
    WebSocket.OPEN -> socket.send(
        Json.encodeToString(
            Report(
                runId = runId,
                uiFramework = uiFramework,
                datasetKey = dataset,
                rung = rungOf(row),
                elements = result.rows,
                built = result.toggled,
                painted = result.painted,
            )
        )
    )

    else -> Unit
}

private fun buttons() = listOf(COMMAND_BUILD_DOM, "expandAll", "collapseAll").map { fixtureElement(it) as HTMLButtonElement }

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

/** Collapse everything below the divisions, in one fixture-owned act. */
private suspend fun collapseAll(button: HTMLButtonElement) {
    button.disabled = true
    val started = performance.now()

    val folded = fixture.foldAll()
    val toggled = performance.now()
    val painted = nextPaint()

    fixtureStatus.textContent = "Collapsed $folded: ${(toggled - started).ms()} toggling, ${(painted - started).ms()} to paint"
    button.disabled = false
}

/** The reveal: unfold the whole tree in survivable chunks, measuring till finish or the browser crash. */
private suspend fun reveal() {
    buttons().forEach { it.disabled = true }

    val row = fixtureElement("reveal") as HTMLTableRowElement
    var revealed = 0
    var toggled = 0.0
    var painted = 0.0
    var chunk: Int

    do {
        onScreen()
        darkened = false

        val started = performance.now()
        chunk = fixture.unfold(revealStepSize)
        revealed += chunk
        val stamp = performance.now()
        val paint = nextPaint()

        if (darkened) {
            fixtureStatus.textContent = "Reveal: BOOM -- tab went dark after ${revealed.grouped()} rows -- accumulation abandoned."
            buttons().forEach { it.disabled = false }
            return
        }

        toggled += stamp - started
        painted += paint - started

        val result = RevealMeasurement(revealed, toggled, painted)
        fillReveal(row, result)
        postReveal(row, result)
        fixtureStatus.textContent = "Reveal: ${revealed.grouped()} rows, ${painted.ms()} to paint"

        breathe()
    } while (chunk > 0)

    fixtureStatus.textContent = "Reveal complete: ${revealed.grouped()} rows, ${painted.ms()} to paint."
    buttons().forEach { it.disabled = false }
}

/**
 * Hand the harness a framework fixture to run. The only entry point.
 *
 * Every framework fixture is driven by identical code.
 */
fun start(frameworkFixture: Fixture) {
    fixture = frameworkFixture

    socket.addEventListener(ON_OPEN, { fixtureStatus.textContent = "Ready." })
    document.addEventListener(EVENT_DOCUMENT_VISIBILITY_CHANGE) { darkened = darkened || hidden }

    fixtureElement(COMMAND_BUILD_DOM).addEventListener(ON_CLICK, { scope.launch { ladder() } })
    fixtureElement("expandAll").addEventListener(ON_CLICK, { scope.launch { reveal() } })
    fixtureElement("collapseAll").addEventListener(ON_CLICK, { event ->
        scope.launch { collapseAll(event.currentTarget as HTMLButtonElement) }
    })

    fixtureRun.textContent = runId.ifEmpty { "-" }
    fixtureElement("datasetKey").textContent = dataset.ifEmpty { "-" }
    (fixtureElement(COMMAND_BUILD_DOM) as HTMLButtonElement).disabled = dataset.isEmpty()
}

/** The container everything renders into. */
@Suppress("unused")
val host: Element get() = fixtureTree
