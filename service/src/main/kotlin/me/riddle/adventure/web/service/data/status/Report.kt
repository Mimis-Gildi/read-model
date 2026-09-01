package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.model.status.MatrixCell

/**
 * Single measurement sent by the fixture.
 */
@Serializable
data class Report(
    val type: String = "report",
    val run: String,
    val module: String,
    val dataset: String,
    val rung: String,
    val elements: Int,
    val built: Double,
    val painted: Double,
) {

    val cell: MatrixCell get() = MatrixCell(listOf(built, painted))
}
