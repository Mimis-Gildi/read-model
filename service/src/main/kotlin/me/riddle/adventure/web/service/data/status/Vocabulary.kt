/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * This doesn't come from prototyping Slop.
 * But also doesn't accomodate different port fixtures yet.
 * AI can't extrapolate and we keep that in mind.
 */

package me.riddle.adventure.web.service.data.status

import me.riddle.adventure.web.model.bench.Dataset
import me.riddle.adventure.web.model.status.DatasetOption
import me.riddle.adventure.web.model.status.UIFrameworkUnderProfiling
import me.riddle.adventure.web.model.status.ModuleOption
import me.riddle.adventure.web.model.status.Vocabulary

/** Control plane and any read model showing this is dictated what it shows: owns no business logic. */
val VOCABULARY: Vocabulary = Vocabulary(
    modules = UIFrameworkUnderProfiling.entries.map { ModuleOption(it.key, it.label) },
    datasets = Dataset.entries.map { DatasetOption(it.key, it.key.replaceFirstChar(Char::uppercase), it.nodes) },
)
