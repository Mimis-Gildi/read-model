/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
package me.riddle.adventure.web.kobweb.fixture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import com.varabyte.kobweb.core.init.InitKobweb
import com.varabyte.kobweb.core.init.InitKobwebContext
import me.riddle.adventure.web.harness.Fixture
import me.riddle.adventure.web.harness.host
import me.riddle.adventure.web.harness.start
import me.riddle.adventure.web.model.*
import me.riddle.adventure.web.model.bench.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.Element

/**
 * This answers :harness [Fixture]
 * me.riddle.adventure.web.harness.Fixture
 * the way all test fixtures do.
 */


/** One level of the ladder: how to reach the children, what to write in the row, whether it ships folded. */
private class Rung(
    val children: (Any) -> List<Any>,
    val label: (Any) -> String,
    val meta: ((Any) -> String)?,
    val collapsed: Boolean,
)

/** CAUTION: the one deliberate erasure, as in `pure.ts`: each level is typed, the walk consumes them by depth. */
@Suppress("UNCHECKED_CAST")
private fun <N> rung(
    children: (N) -> List<Any> = { emptyList() },
    label: (N) -> String,
    meta: ((N) -> String)? = null,
    collapsed: Boolean = false,
) = Rung(children as (Any) -> List<Any>, label as (Any) -> String, meta as ((Any) -> String)?, collapsed)

private val RUNGS = listOf(
    rung<CorporateDivision>({ it.groups }, { it.division }),
    rung<CorporateGroup>({ it.teams }, { it.group }),

    // Teams ship folded!
    rung<ProductTeam>({ it.people }, { it.team }, collapsed = true),
    rung<Person>(
        label = { "${it.firstName} ${it.lastName}" },
        meta = { "${it.jobTitle.padEnd(26)}${it.location.padEnd(18)}${it.phone}" },
    ),
)

/** A foldable node's only moving part, and the rows it hides -- the unit the harness's chunk size is counted in. */
private class Fold(val index: Int, val depth: Int, val rows: Int, val closed: MutableState<Boolean>)

/** Registered by the initial composition, so in document order: what `toggleChunk` walks. */
private val folds = mutableListOf<Fold>()

private fun count(quantity: Int): String = quantity.asDynamic().toLocaleString() as String

@Composable
private fun Node(node: Any, depth: Int) {
    val level = RUNGS[depth]
    val children = level.children(node)
    val fold = if (children.isEmpty()) null
    else remember { Fold(folds.size, depth, children.size, mutableStateOf(level.collapsed)).also(folds::add) }
    val closed = fold?.closed?.value == true

    Div({
        attr("class", if (closed) "node depth-$depth collapsed" else "node depth-$depth")
        if (fold != null) attr("data-fold", fold.index.toString())
    }) {
        Div({ attr("class", "row") }) {
            Span({ attr("class", "twist") }) { Text(if (children.isEmpty()) ICON_LEAF else if (closed) ICON_SHUT else ICON_OPEN) }
            Span({ attr("class", "name") }) { Text(level.label(node)) }
            Span({ attr("class", "meta") }) { Text(level.meta?.invoke(node) ?: count(children.size)) }
        }
        if (children.isNotEmpty() && !closed) Div({ attr("class", "kids") }) { children.forEach { Node(it, depth + 1) } }
    }
}

/** Writes the folds, then recomposes and applies NOW instead of on the next frame. */
private fun ControlledComposition.toggle(changed: List<Fold>, closed: Boolean) {
    if (changed.isEmpty()) return
    changed.forEach { it.closed.value = closed }
    recordModificationsOf(changed.map { it.closed }.toSet())
    val snapshot = Snapshot.takeMutableSnapshot({ recordReadOf(it) }, { recordWriteOf(it) })
    val recomposed = try {
        snapshot.enter { recompose() }
    } finally {
        snapshot.apply().check()
        snapshot.dispose()
    }
    if (recomposed) applyChanges()
}

/** Teams now `!closed`, in document order, up to and including the one that carries the rows past [limit]. */
private fun ControlledComposition.toggleChunk(limit: Int, closed: Boolean): Int =
    folds.asSequence().filter { it.depth == 2 && it.closed.value != closed }
        .fold(0 to emptyList<Fold>()) { (rows, steps), fold ->
            if (rows >= limit) rows to steps else rows + fold.rows to steps + fold
        }
        .also { (_, steps) -> toggle(steps, closed) }
        .first

object KobwebFixture : Fixture {
    /** Outside every clock: disposed on reset, so each build is a first composition, not a diff. */
    private var composition: ControlledComposition? = null

    override val uiFramework: String = UI_FRAMEWORK_KOBWEB.first

    /** The initial composition is applied synchronously, so the line after it is a meaningful `built` stamp. */
    override fun build(company: Company): Int {
        composition = renderComposable(root = host) { company.divisions.forEach { Node(it, 0) } } as ControlledComposition
        return host.querySelectorAll("*").length
    }

    override fun reset() {
        composition?.dispose()
        composition = null
        folds.clear()
        host.asDynamic().replaceChildren()
    }

    override fun unfold(count: Int): Int = composition?.toggleChunk(count, false) ?: 0

    override fun fold(count: Int): Int = composition?.toggleChunk(count, true) ?: 0

    fun click(index: Int) = folds[index].let { composition?.toggle(listOf(it), !it.closed.value) }
}

/** Kobweb calls this once, before its own (empty) page composes into the hidden `#_kobweb-root`. */
@InitKobweb
fun initFixture(@Suppress("UNUSED_PARAMETER") ctx: InitKobwebContext) {
    host.addEventListener(ON_CLICK, { event ->
        (event.target as? Element)?.closest(".node")
            ?.getAttribute("data-fold")?.toInt()
            ?.let(KobwebFixture::click)
    })
    start(KobwebFixture)
}
