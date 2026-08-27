package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Level

/**
 * What the control plane may ask for. Pushed once on connection, before the [Matrix],
 * so the page never holds an opinion about which datasets exist, the read model proper,
 * and the same doctrine the matrix already follows: the service composes it,
 * the page renders what it is told.
 *
 * [DatasetOption.key] is what goes on the URL; [DatasetOption.nodes]
 * is shown beside the label so the size of the rung is visible at the point of choosing it.
 */
@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

@Serializable
data class Vocabulary(
    val type: String = "vocabulary",
    val datasets: List<DatasetOption>,
    /** The rungs of the ladder, in depth order -- so a fixture's results table is titled by the service too. */
    val levels: List<String>,
)

/**
 * Composed straight off [Dataset.entries], so the control plane cannot name a dataset the routes would 404 on.
 *
 * The label is derived rather than stored: it is the key capitalized,
 * and a stored one would be a third copy of the same word.
 */
val VOCABULARY: Vocabulary = Vocabulary(
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
    levels = Level.entries.map { it.label },
)
