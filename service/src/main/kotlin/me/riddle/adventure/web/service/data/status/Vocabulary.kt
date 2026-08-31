package me.riddle.adventure.web.service.data.status

import kotlinx.serialization.Serializable
import me.riddle.adventure.web.service.data.bench.Dataset

/**
 * What the reporting read model wants to know.
 */
@Serializable
data class DatasetOption(val key: String, val label: String, val nodes: Int)

/** ToDo: For now, key is the mount point too; may change for kobweb. */
@Serializable
data class ModuleOption(val key: String, val label: String)

@Serializable
data class Vocabulary(
    val type: String = "vocabulary",
    val modules: List<ModuleOption>,
    val datasets: List<DatasetOption>,
)

/** Control plane and any read model showing this is dictated what it shows: owns no business logic. */
val VOCABULARY: Vocabulary = Vocabulary(
    modules = Module.entries.map { ModuleOption(it.key, it.label) },
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
)
