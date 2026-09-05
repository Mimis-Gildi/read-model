/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * FixMe: Deslopification refactoring pending. */

@file:JsExport

package me.riddle.adventure.web.harness

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import me.riddle.adventure.web.model.bench.Company
import me.riddle.adventure.web.model.status.Report
import org.w3c.dom.*
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event
import org.w3c.dom.url.URLSearchParams
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * The measurement harness. Shared, verbatim, by every module under test.
 *
 * This is only TWO things:
 *
 * 1. Contract we use in measuring materialized by the fixture.
 * 2. Commands a fixture expects to receive and process.
 *
 */

// ====

/**
 * For what the module supplies; see [start].
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

    /** Tears the previous rung down outside any clock: same DOM. React will uniquely crash here also! */
    fun reset()

    /** Unfolds the next chunk of up to [count] folded `Person` nodes. Returns how many it actually unfolded. */
    fun unfold(count: Int): Int

    /** Folds every currently unfolded `Person` node. Returns how many it folded. */
    fun fold(): Int
}

/** One measured render. */
data class Measurement(val elements: Int, val built: Double, val painted: Double)

private external interface Performance {
    fun now(): Double
}

/** The global clock; there is no typed `window.performance` to lean on across browser bindings. */
private external val performance: Performance

// @formatter:off
private val params              = URLSearchParams(window.location.search)
private val dataset             = params.get("dataset").orEmpty()
private val run                 = params.get("run").orEmpty()
private val module              = params.get("module").orEmpty()
private val REVEAL_STEP         = params.get("step")?.toIntOrNull() ?: 20_000   // Experimentation derived
private val REVEAL_PAUSE        = params.get("pause")?.toLongOrNull() ?: 36L   // Experimentation derived
// @formatter:on

private fun fixtureElement(id: String) = document.getElementById(id) as HTMLElement
private val fixtureTree by lazy { fixtureElement("tree") }
private val fixtureRun by lazy { fixtureElement("run") }
private val fixtureStatus by lazy { fixtureElement("status") }
private val fixtureRows by lazy { fixtureElement("rows") }


/** Two escapes into JS number formatting: the platform has no Kotlin equivalent of either. */
private fun Int.grouped(): String = asDynamic().toLocaleString() as String
private fun Double.ms(): String = "${asDynamic().toFixed(1) as String} ms"


/** The bindings carry no indexed access on a live collection, and no `hidden` on the document. */
private fun NodeList.elements(): List<Element> = (0 until length).mapNotNull { item(it) as? Element }
private fun HTMLTableRowElement.cell(at: Int) = cells.item(at) as? HTMLElement

private val hidden: Boolean get() = document.asDynamic().hidden as Boolean

private val scope = CoroutineScope(Dispatchers.Main)

/** The meat: module under test, handed over by [start]. */
private lateinit var fixture: Fixture


/** The pause between reveal chunks. Deliberately outside every clock to allow the browser to recover off of the Cliff. */
private suspend fun breathe() = delay(REVEAL_PAUSE.milliseconds)

/** Resolves on the frame after the one the caller's DOM work completes (once it is painted). */
@Suppress("NON_EXPORTABLE_TYPE")
private suspend fun nextPaint(): Double = suspendCancellableCoroutine { waiting ->
    window.requestAnimationFrame {
        window.requestAnimationFrame { waiting.resume(performance.now()) }
    }
}

/** IMPORTANT: Saving Grace - a way to survive the runaway "Main Thread Starvation" that'd kill the experiment. */
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
                else -> document.removeEventListener("visibilitychange", seen).also { waiting.resume(Unit) }
            }
        }
        document.addEventListener("visibilitychange", seen)
    }
}

/** The tree culled to [level] outside the clock because transport is not part of this experiment. */
private suspend fun fetchLevel(level: Int): Company =
    window.fetch("/data/$dataset/$level", js("({})")).await().let { response ->
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
 * Teardown of the previous rung happens BEFORE `started` -- time NOT from here.
 * This level is NEVER charged for the DOM the level before it has left behind.
 *
 * IMPORTANT: a tab hidden mid-measurement stops painting! Any benchmark attempts are a moot point then.
 * I once had 60 elements reporting 106,843.9 ms this way. I found no practical way to salvage that.
 * AND: This is the best recovery point I discovered: just fold and unfold over live caches again.
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
    listOf(nodes.grouped(), result.elements.grouped(), result.built.ms(), result.painted.ms())
        .forEachIndexed { column, value -> row.cell(column + 1)?.textContent = value }

/**
 * The ladder's rungs are read off the Ktor (service) result matrix in the depth order.
 */
private fun ladderRows() = fixtureRows.querySelectorAll("tr[id^=\"level-\"]").elements()
    .filterIsInstance<HTMLTableRowElement>()

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
private val socket by lazy { WebSocket("ws://${window.location.host}/ws") }

/** Post a finished rung at the time no measurement is going on. */
private fun post(row: HTMLTableRowElement, result: Measurement) = when (socket.readyState) {
    WebSocket.OPEN -> socket.send(
        Json.encodeToString(
            Report(
                run = run,
                module = module,
                dataset = dataset,
                rung = rungOf(row),
                elements = result.elements,
                built = result.built,
                painted = result.painted,
            )
        )
    )

    else -> Unit
}

private fun buttons() = listOf("start", "expandAll", "collapseAll").map { fixtureElement(it) as HTMLButtonElement }

/** Model nodes of whatever the ladder last put on the page for reveal to lay these out. */
private var nodesOnScreen = 0

/** Teardown the ladder: the time [measure] spent in `fixture.reset`; i.e., React core weakness. */
private var teardown = 0.0

/** The ladder: level 0 through 3, each fetched then measured, reported as it completes. */
private suspend fun ladder() {
    buttons().forEach { it.disabled = true }
    fixtureRows.querySelectorAll("td:not(:first-child)").elements().forEach { it.textContent = "-" }
    teardown = 0.0

    if (hidden) fixtureStatus.textContent =
        "Waiting: bring this tab to the front -- paint cannot be measured in a background tab."

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

    val folded = fixture.fold()
    val toggled = performance.now()
    val painted = nextPaint()

    fixtureStatus.textContent =
        "Collapsed $folded: ${(toggled - started).ms()} toggling, ${(painted - started).ms()} to paint"
    button.disabled = false
}

/** The reveal: unfold the whole tree in survivable chunks, measuring till finish or the browser crash. */
private suspend fun reveal() {
    buttons().forEach { it.disabled = true }

    val row = fixtureElement("reveal") as HTMLTableRowElement
    var revealed = 0
    var built = 0.0
    var painted = 0.0
    var chunk: Int

    do {
        onScreen()
        darkened = false

        val started = performance.now()
        chunk = fixture.unfold(REVEAL_STEP)
        revealed += chunk
        val toggled = performance.now()
        val stamp = nextPaint()

        if (darkened) {
            fixtureStatus.textContent =
                "Reveal: BOOM -- tab went dark after ${revealed.grouped()} rows -- accumulation abandoned."
            buttons().forEach { it.disabled = false }
            return
        }

        built += toggled - started
        painted += stamp - started

        val result = Measurement(revealed, built, painted)
        fill(row, nodesOnScreen, result)
        post(row, result)
        fixtureStatus.textContent = "Reveal: ${revealed.grouped()} rows, ${painted.ms()} to paint"

        breathe()
    } while (chunk > 0)

    fixtureStatus.textContent = "Reveal complete: ${revealed.grouped()} rows, ${painted.ms()} to paint."
    buttons().forEach { it.disabled = false }
}

/**
 * Hand the harness a module to run. The only entry point.
 *
 * Every module is driven by identical code.
 */
fun start(module: Fixture) {
    fixture = module

    document.addEventListener("visibilitychange", { darkened = darkened || hidden })

    fixtureElement("start").addEventListener("click", { scope.launch { ladder() } })
    fixtureElement("expandAll").addEventListener("click", { scope.launch { reveal() } })
    fixtureElement("collapseAll").addEventListener("click", { event ->
        scope.launch { collapseAll(event.currentTarget as HTMLButtonElement) }
    })

    fixtureRun.textContent = run.ifEmpty { "-" }
    fixtureElement("datasetKey").textContent = dataset.ifEmpty { "-" }
    fixtureStatus.textContent = if (dataset.isEmpty()) "No dataset on the URL." else "Ready."
    (fixtureElement("start") as HTMLButtonElement).disabled = dataset.isEmpty()
}

/** The container everything renders into. */
val host: Element get() = fixtureTree
