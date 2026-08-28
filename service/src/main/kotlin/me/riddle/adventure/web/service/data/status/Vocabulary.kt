package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset

/**
 * What the control plane may ask for. Pushed once on connection, before the [Matrix], so the page never holds an
 * opinion about which modules or datasets exist.
 *
 * [key] goes on the URL; [nodes] is shown beside the label, so the size is visible at the point of choosing.
 */
@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

/** [key] is both the mount path and what the fixture puts on the wire; [label] is what the operator reads. */
@Serializable
data class ModuleOption(val key: String, val label: String)

@Serializable
data class Vocabulary(
    val type: String = "vocabulary",
    val modules: List<ModuleOption>,
    val datasets: List<DatasetOption>,
)

/** Composed straight off the enums, so the control plane cannot name a module or dataset the routes would 404 on. */
val VOCABULARY: Vocabulary = Vocabulary(
    modules = Module.entries.map { ModuleOption(it.key, it.label) },
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
)
