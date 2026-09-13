/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */


@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/** Raw values only -- formatting happens in the page. */
@Serializable
data class PerformanceComparisonValues(val values: List<Double> = emptyList())

@Serializable
data class PerformanceComparisonCategoryRow(val dataset: String, val title: String, val cells: List<PerformanceComparisonValues>)

@Serializable
data class PerformanceComparisonTable(
    val type: String = "Performance Comparison Table",
    val columns: List<String>,
    val rows: List<PerformanceComparisonCategoryRow>,
    val totals: List<PerformanceComparisonCategoryRow> = emptyList(),
)

const val TOTAL: String = "Total"
