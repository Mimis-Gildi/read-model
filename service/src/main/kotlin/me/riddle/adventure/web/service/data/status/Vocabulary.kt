package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset
import me.riddle.adventure.web.service.data.bench.Level

/**
 * What the control plane may ask for. Pushed once on connection, before the [Matrix], so the page never holds an
 * opinion about which datasets exist.
 *
 * [key] goes on the URL; [nodes] is shown beside the label, so the size is visible at the point of choosing.
 */
@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

@Serializable
data class Vocabulary(
    val type: String = "vocabulary",
    val datasets: List<DatasetOption>,
    /** The rungs of the ladder, in depth order. */
    val levels: List<String>,
)

/** Composed straight off [Dataset.entries], so the control plane cannot name a dataset the routes would 404 on. */
val VOCABULARY: Vocabulary = Vocabulary(
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
    levels = Level.entries.map { it.label },
)
