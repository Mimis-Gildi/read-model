package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * One measured rung, sent by a fixture the moment it finishes climbing it.
 *
 * Per rung rather than per run: a fixture that dies at LOAD never sends a summary, so the last rung that arrived
 * before the silence *is* the failure point. The server is the black-box recorder.
 *
 * [rung] is a [Rung] label rather than a depth, because the reveal has no depth. The reveal may also report many times
 * against the same address -- each chunk carries the accumulation so far, so the cell holds the last chunk that survived.
 *
 * [built] and [painted] are milliseconds: the tree constructed and attached, and the frame the browser actually
 * painted it in. See the fixture's header for the boundary they measure.
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

    /** The two readings as numbers, so the column total can add them. The page does the formatting. */
    val cell: MatrixCell get() = MatrixCell(listOf(built, painted))
}
