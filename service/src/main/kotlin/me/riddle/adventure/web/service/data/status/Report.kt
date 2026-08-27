package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable

/**
 * One measured rung, sent by a fixture the moment it finishes climbing it.
 *
 * A fixture sends a **measurement, not a matrix**. It knows what it rendered and how long that took; it knows nothing
 * about columns, rounding, or what the other frameworks did. Composing those into what the control plane renders is the
 * service's job -- the same doctrine [Matrix] and [Vocabulary] already follow.
 *
 * Reporting per rung rather than per run is deliberate: a fixture that dies at LOAD never sends a summary, so the last
 * rung that arrived before the silence *is* the failure point. The server is the black-box recorder.
 *
 * [built] and [painted] are milliseconds, and both are kept because "rendered" has two honest readings -- the tree
 * constructed and attached, and the frame in which the browser has actually laid it out and painted it. See the
 * fixture's header for the boundary they measure.
 */
@Serializable
data class Report(
    val type: String = "report",
    val run: String,
    val module: String,
    val dataset: String,
    val level: Int,
    val elements: Int,
    val built: Double,
    val painted: Double,
) {

    /**
     * The pair as the matrix shows it: `built / painted`, one decimal, no unit.
     *
     * Formatted here rather than in JavaScript because [MatrixRow] cells are pre-formatted strings on purpose -- the
     * rounding and the vocabulary live server-side, in one place, for all three fixtures.
     */
    val cell: String get() = "%.1f / %.1f".format(built, painted)
}
