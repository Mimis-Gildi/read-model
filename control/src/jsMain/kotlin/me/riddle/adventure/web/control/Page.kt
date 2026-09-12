/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no remaining prototyping slop.
 */

package me.riddle.adventure.web.control

import kotlinx.browser.document
import me.riddle.adventure.web.model.PARAMETER_DATASET
import me.riddle.adventure.web.model.PARAMETER_UI_FRAMEWORK
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement

/** Every element on the page resolves once against `control/index.html`. */
object Page {

    val table: HTMLTableElement = element("matrix")
    val head: HTMLTableRowElement = element("head")
    val rows: HTMLElement = element("rows")
    val foot: HTMLElement = element("foot")
    val empty: HTMLElement = element("empty")
    val conn: HTMLElement = element("conn")
    val connText: HTMLElement = element("conn-text")
    val launched: HTMLElement = element("launched")
    val module: HTMLSelectElement = element(PARAMETER_UI_FRAMEWORK)
    val dataset: HTMLSelectElement = element(PARAMETER_DATASET)
    val launch: HTMLElement = element("launch")
}

private fun <T : Element> element(id: String): T =
    document.getElementById(id)?.unsafeCast<T>() ?: error("control/index.html carries no #$id")

fun <T : Element> create(tag: String): T = document.createElement(tag).unsafeCast<T>()
