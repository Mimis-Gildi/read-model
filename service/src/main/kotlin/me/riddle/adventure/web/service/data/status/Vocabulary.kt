package me.riddle.adventure.web.service.data.status

import me.riddle.adventure.web.model.status.DatasetOption
import me.riddle.adventure.web.model.status.ModuleOption
import me.riddle.adventure.web.model.status.Vocabulary
import me.riddle.adventure.web.service.data.bench.Dataset

/** Control plane and any read model showing this is dictated what it shows: owns no business logic. */
val VOCABULARY: Vocabulary = Vocabulary(
    modules = Module.entries.map { ModuleOption(it.key, it.label) },
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
)
