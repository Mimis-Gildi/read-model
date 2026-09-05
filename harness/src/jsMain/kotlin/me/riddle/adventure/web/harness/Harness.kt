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
import org.w3c.fetch.RequestInit
import kotlin.coroutines.resume

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

private fun el(id: String) = document.getElementById(id) as HTMLElement
private val elTree by lazy { el("tree") }
private val elRun by lazy { el("run") }
private val elStatus by lazy { el("status") }
private val elRows by lazy { el("rows") }

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

/**
 * Run a chunk of folding and do not return until it has landed.
 *
 * A module that folds by touching the DOM is finished the moment its loop returns: i.e, `work()`.
 * IMPORTANT: a module that folds by asking a framework to re-render is NOT!
 * EXAMPLE: react.dev is explicit that a render is only "scheduled", and the harness stamps a chunk with a double
 * `requestAnimationFrame` on the assumption that the work is done by then. That assumption is the module's to "accept."
 * The module gets the hook and decides -- React supplies `flushSync`; my Pure JS needs nothing.
 *
 * One flush per chunk, never per node: per node would be too many separate synchronous renders -- a problem:
 * - such numbers no real React application should ever produce (except at a laggard who asked me this question)
 * - and the slowdown is cascaded and exponential, formally "DOM Size Performance Cliff."
 *
 * That drop-off is caused by "Layout Thrashing," and it is "Forced Synchronous Layout Wall" in browser docs. As I will
 * show on Demoscene, an article and maybe a video for this -- the synthetic "Main Thread Starvation" makes any benching
 * totally useless because the browser process container is already in the compromised state: not a framework artifact!
 *
 * This is WHY I chose to survive the cliff by chunking and recovering in the first place.
 */
/** The pause between reveal chunks. Deliberately outside every clock to allow the browser to recover off of the Cliff. */
private suspend fun breathe() = delay(REVEAL_PAUSE)

/** Resolves on the frame after the one the caller's DOM work completes (once it is painted). */
private suspend fun nextPaint(): Double = suspendCancellableCoroutine { waiting ->
    window.requestAnimationFrame {
        window.requestAnimationFrame { waiting.resume(performance.now()) }
    }
}

/**
 * IMPORTANT: Saving Grace - a way to survive the runaway "Main Thread Starvation" that'd kill the experiment.
 *
 * Set the moment the tab goes dark, so a measurement in flight knows it is spoiled.
 *
 * Chrome does not run `requestAnimationFrame` in a hidden tab, so [nextPaint] never settles and a ladder started
 * in the background hangs forever -- this is observed through purpose-built fixtures I'd experimented with prior.
 * Waiting is also the honest behavior rather than a nicety because a background tab is throttled, so any paint number
 * measured in one would be pointless garbage. With that in mind, the machine's performance is also not uniform.
 *
 * Measurements are RELATIVE to one another.
 */
private var darkened = false

/** Resolves once the tab is actually on screen. An opportunity to inject some recovery code. */
private suspend fun onScreen() = when {
    !hidden -> Unit
    else -> suspendCancellableCoroutine<Unit> { waiting ->
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
    window.fetch("/data/$dataset/$level", RequestInit()).await().let { response ->
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
            elStatus.textContent = "Level $level: BOOM -- tab went dark mid-render -- discarded, re-running."
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
private fun ladderRows() = elRows.querySelectorAll("tr[id^=\"level-\"]").elements()
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

private fun buttons() = listOf("start", "expandAll", "collapseAll").map { el(it) as HTMLButtonElement }

/** Model nodes of whatever the ladder last put on the page for reveal to lay these out. */
private var nodesOnScreen = 0

/** Teardown the ladder: the time [measure] spent in `fixture.reset`; i.e., React core weakness. */
private var teardown = 0.0

/** The ladder: level 0 through 3, each fetched then measured, reported as it completes. */
private suspend fun ladder() {
    buttons().forEach { it.disabled = true }
    elRows.querySelectorAll("td:not(:first-child)").elements().forEach { it.textContent = "–" }
    teardown = 0.0

    if (hidden) elStatus.textContent =
        "Waiting: bring this tab to the front -- paint cannot be measured in a background tab."

    ladderRows().forEachIndexed { level, row ->
        elStatus.textContent = "Level $level: fetching…"
        val company = fetchLevel(level)
        elStatus.textContent = "Level $level: rendering…"
        val result = measure(company, level)
        nodesOnScreen = census(company)
        fill(row, nodesOnScreen, result)
        post(row, result)
    }

    elStatus.textContent = "Ladder complete. ${teardown.ms()} of teardown, billed to no rung."
    buttons().forEach { it.disabled = false }
}

/** Collapse everything below the divisions, in one fixture-owned act. */
private suspend fun collapseAll(button: HTMLButtonElement) {
    button.disabled = true
    val started = performance.now()

    val folded = fixture.fold()
    val toggled = performance.now()
    val painted = nextPaint()

    elStatus.textContent =
        "Collapsed $folded: ${(toggled - started).ms()} toggling, ${(painted - started).ms()} to paint"
    button.disabled = false
}

/** The reveal: unfold the whole tree in survivable chunks, measuring till finish or the browser crash. */
private suspend fun reveal() {
    buttons().forEach { it.disabled = true }

    val row = el("reveal") as HTMLTableRowElement
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
            elStatus.textContent =
                "Reveal: BOOM -- tab went dark after ${revealed.grouped()} rows -- accumulation abandoned."
            buttons().forEach { it.disabled = false }
            return
        }

        built += toggled - started
        painted += stamp - started

        val result = Measurement(revealed, built, painted)
        fill(row, nodesOnScreen, result)
        post(row, result)
        elStatus.textContent = "Reveal: ${revealed.grouped()} rows, ${painted.ms()} to paint"

        breathe()
    } while (chunk > 0)

    elStatus.textContent = "Reveal complete: ${revealed.grouped()} rows, ${painted.ms()} to paint."
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

    el("start").addEventListener("click", { scope.launch { ladder() } })
    el("expandAll").addEventListener("click", { scope.launch { reveal() } })
    el("collapseAll").addEventListener("click", { event ->
        scope.launch { collapseAll(event.currentTarget as HTMLButtonElement) }
    })

    elRun.textContent = run.ifEmpty { "–" }
    el("datasetKey").textContent = dataset.ifEmpty { "–" }
    elStatus.textContent = if (dataset.isEmpty()) "No dataset on the URL." else "Ready."
    (el("start") as HTMLButtonElement).disabled = dataset.isEmpty()
}

/** The container everything renders into. */
val host: Element get() = elTree
