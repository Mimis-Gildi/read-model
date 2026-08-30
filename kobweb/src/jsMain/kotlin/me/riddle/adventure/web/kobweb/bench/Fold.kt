/*
 * CAUTION: folding writes to the DOM Compose built, which is only safe because nothing here reads Compose state.
 * `Tree` composes from parsed JSON, so no recomposition is ever scheduled and no attribute is ever reapplied.
 * The moment a node's class or glyph comes from a `remember`, this file is wrong and the fold has to move into state.
 *
 * `pure.js:83` refuses per-node listeners: 195,312 of them at LOAD, attached inside the clock and billed as render
 * cost. One delegated listener on the host costs nothing there, which is what keeps the columns comparable.
 */
package me.riddle.adventure.web.kobweb.bench

import org.w3c.dom.Element
import org.w3c.dom.events.Event

fun shut(box: Element, closed: Boolean) {
    box.classList.toggle("collapsed", closed)
    box.querySelector(":scope > .row > .twist")?.textContent = if (closed) SHUT else OPEN
}

fun foldable(event: Event): Element? = event.target.asDynamic()
    .closest(".node").unsafeCast<Element?>()
    ?.takeIf { it.querySelector(":scope > .kids") != null }

fun Element.folded(): Boolean = classList.contains("collapsed")
