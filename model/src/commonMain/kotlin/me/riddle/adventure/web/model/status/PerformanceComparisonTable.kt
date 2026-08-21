/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */


@file:JsExport

package me.riddle.adventure.web.model.status

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/** Raw values only -- formatting happens in the page. */
@Serializable
data class PerformanceComparisonValues(val values: List<Double> = emptyList())

@Serializable
data class PerformanceComparisonCategoryRow(val dataset: String, val title: String, val cells: List<PerformanceComparisonValues>)

@Serializable
@SerialName("performanceComparisonTable")
data class PerformanceComparisonTable(
    val columns: List<String>,
    val rows: List<PerformanceComparisonCategoryRow>,
    val totals: List<PerformanceComparisonCategoryRow> = emptyList(),
) : Frame

const val TOTAL: String = "Total"
