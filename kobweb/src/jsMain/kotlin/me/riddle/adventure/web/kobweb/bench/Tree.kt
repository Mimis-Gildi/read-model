/*
 * The DOM is the cross-framework contract: the harness queries `.node.collapsed`, `:scope > .kids` and
 * `:scope > .row > .twist`, so this shape has to match `pure.js` element for element or the columns are not comparable.
 *
 * CAUTION: the class list is one `attr` rather than `classes()`, matching `pure.js`'s `node.className` -- one string,
 * one write, no per-token work inside the clock.
 */
package me.riddle.adventure.web.kobweb.bench

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun Tree(company: dynamic) = company.divisions.unsafeCast<Array<dynamic>>().forEach { Node(it, 0) }

@Composable
private fun Node(node: dynamic, depth: Int) {
    val level = LEVELS[depth]
    val children = kids(node, depth)
    val closed = level.collapsed && children.isNotEmpty()

    Div(attrs = { attr("class", if (closed) "node depth-$depth collapsed" else "node depth-$depth") }) {
        Div(attrs = { attr("class", "row") }) {
            Span(attrs = { attr("class", "twist") }) {
                Text(if (children.isEmpty()) LEAF else if (closed) SHUT else OPEN)
            }
            Span(attrs = { attr("class", "name") }) { Text(level.label(node)) }
            Span(attrs = { attr("class", "meta") }) { Text(level.meta(node, children.size)) }
        }
        if (children.isNotEmpty()) {
            Div(attrs = { attr("class", "kids") }) { children.forEach { Node(it, depth + 1) } }
        }
    }
}
