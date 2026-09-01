/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */


package me.riddle.adventure.web.model.status

import kotlinx.serialization.Serializable

/** Each row measurement is the captured numbers to be added up in a column. Formatting happens in the page. */
@Serializable
data class MatrixCell(val values: List<Double> = emptyList())

@Serializable
data class MatrixRow(val dataset: String, val title: String, val cells: List<MatrixCell>)

@Serializable
data class Matrix(
    val type: String = "matrix",
    val columns: List<String>,
    val rows: List<MatrixRow>,
    val totals: List<MatrixRow> = emptyList(),
)

const val TOTAL: String = "Total"
