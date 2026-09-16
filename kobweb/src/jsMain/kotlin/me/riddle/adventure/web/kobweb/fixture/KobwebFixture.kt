/*
 * Kobweb fixture -- Compose HTML builds the tree, the harness measures it.
 *
 * CAUTION: folding writes to the DOM Compose built, which is only safe because the tree reads no Compose state:
 * no recomposition is ever scheduled, no attribute ever reapplied. Idiomatic state folding is a later step.
 */
package me.riddle.adventure.web.kobweb.fixture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
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
import org.w3c.dom.asList

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

private const val KIDS = ":scope > .kids"
private const val TWIST = ":scope > .row > .twist"
private const val FOLDED_TEAMS = ".node.depth-2.collapsed"
private const val OPEN_TEAMS = ".node.depth-2:not(.collapsed)"

private fun count(quantity: Int): String = quantity.asDynamic().toLocaleString() as String

@Composable
private fun Node(node: Any, depth: Int) {
    val level = RUNGS[depth]
    val children = level.children(node)
    val closed = level.collapsed && children.isNotEmpty()

    Div({ attr("class", if (closed) "node depth-$depth collapsed" else "node depth-$depth") }) {
        Div({ attr("class", "row") }) {
            Span({ attr("class", "twist") }) { Text(if (children.isEmpty()) ICON_LEAF else if (closed) ICON_SHUT else ICON_OPEN) }
            Span({ attr("class", "name") }) { Text(level.label(node)) }
            Span({ attr("class", "meta") }) { Text(level.meta?.invoke(node) ?: count(children.size)) }
        }
        if (children.isNotEmpty()) Div({ attr("class", "kids") }) { children.forEach { Node(it, depth + 1) } }
    }
}

private fun shut(nodeElement: Element, closed: Boolean) {
    nodeElement.classList.toggle("collapsed", closed)
    nodeElement.querySelector(TWIST)!!.textContent = if (closed) ICON_SHUT else ICON_OPEN
}

private fun rowsOf(nodeElement: Element): Int = nodeElement.querySelector(KIDS)?.childElementCount ?: 0

/** Nodes matching [selector], in document order, up to and including the one that carries the rows past [limit]. */
private fun toggleChunk(selector: String, limit: Int, closed: Boolean): Int =
    host.querySelectorAll(selector).asList().map { it as Element }
        .fold(0 to emptyList<Element>()) { (rows, steps), element ->
            if (rows >= limit) rows to steps else rows + rowsOf(element) to steps + element
        }
        .also { (_, steps) -> steps.forEach { shut(it, closed) } }
        .first

object KobwebFixture : Fixture {
    /** Outside every clock: disposed on reset, so each build is a first composition, not a diff. */
    private var composition: Composition? = null

    override val uiFramework: String = UI_FRAMEWORK_KOBWEB.first

    /** The initial composition is applied synchronously, so the line after it is a meaningful `built` stamp. */
    override fun build(company: Company): Int {
        composition = renderComposable(root = host) { company.divisions.forEach { Node(it, 0) } }
        return host.querySelectorAll("*").length
    }

    override fun reset() {
        composition?.dispose()
        composition = null
        host.asDynamic().replaceChildren()
    }

    override fun unfold(count: Int): Int = toggleChunk(FOLDED_TEAMS, count, false)

    override fun fold(count: Int): Int = toggleChunk(OPEN_TEAMS, count, true)
}

/** Kobweb calls this once, before its own (empty) page composes into the hidden `#_kobweb-root`. */
@InitKobweb
fun initFixture(@Suppress("UNUSED_PARAMETER") ctx: InitKobwebContext) {
    host.addEventListener(ON_CLICK, { event ->
        (event.target as? Element)?.closest(".node")
            ?.takeIf { it.querySelector(KIDS) != null }
            ?.let { shut(it, !it.classList.contains("collapsed")) }
    })
    start(KobwebFixture)
}
