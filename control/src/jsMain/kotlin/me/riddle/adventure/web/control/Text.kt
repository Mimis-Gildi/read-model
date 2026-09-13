/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: no prototyping slop remaining.
 */

package me.riddle.adventure.web.control

import me.riddle.adventure.web.model.status.PerformanceComparisonValues
import kotlin.js.json

const val BLANK: String = "-"

/** The reference `Par` column is styled differently. */
fun isPar(column: String): Boolean = column.trim().lowercase() == "par"

/** Milliseconds, grouped so the thousand mark reads as the "second" boundary. */
fun timeMsText(value: Double): String = value.asDynamic()
    .toLocaleString("en-US", json("minimumFractionDigits" to 1, "maximumFractionDigits" to 1))
    .unsafeCast<String>()

fun countText(value: Int): String = value.asDynamic().toLocaleString().unsafeCast<String>()

/** A body cell shows its readings as they were measured: build and paint. */
fun cellText(cell: PerformanceComparisonValues?): String =
    cell?.values?.takeIf { it.isNotEmpty() }?.joinToString(" / ", transform = ::timeMsText) ?: BLANK

/** A foot cell is all the fragments added together to show total time. */
fun totalText(cell: PerformanceComparisonValues?): String =
    cell?.values?.takeIf { it.isNotEmpty() }?.sum()?.let(::timeMsText) ?: BLANK
