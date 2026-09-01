/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Not slop origin.
 */
package me.riddle.adventure.web.service.data.service

import me.riddle.adventure.web.model.bench.*

/**
 * The service responsible for:
 * 1) generating and
 * 2) serving
 * [Company] loading data.
 *
 * Culling follows datasets' hierarchical representations of a company's organizational structure:
 *
 * - Divisions,
 * - Groups,
 * - Teams, and
 * - People,
 *
 * According to specified configurations.
 *
 * This data is:
 *
 * 1. Reproducible -- each new generation creates the exact same data.
 * 2. Repeatable -- a larger dataset contains a smaller and is an extension to it.
 *
 * Provided library versions remain the same.
 */
class HumanResourcesDataService {

    private val sets by lazy { Dataset.entries.associateWith { generate(it) } }

    fun get(dataset: Dataset): Company = sets.getValue(dataset)

    fun get(dataset: Dataset, level: Level): Company = get(dataset).cullTo(level)

    /**
     * The [Level] is the deepest type descended to.
     * Everything deeper than the level is empty.
     *
     * Nothing is ever regenerated after `init`. All layers are the same instance projections.
     *
     * This has a very specific purpose in UX/UI testing so that my godly Pure JS is benched against:
     *
     * 1. Chrome's natural crash on data fetch with and without Pretty-print;
     * 2. Treats React and other fixtures on equal footing.
     *
     * Bench results are going into the associated article.
     * See [Web Showdown 2026: Read Model](https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html "Poor laggard's treasure.")
     */
    private fun Company.cullTo(level: Level): Company = when (level) {
        Level.PEOPLE -> copy()

        Level.DIVISIONS -> copy(divisions = divisions.map { it.copy(groups = emptyList()) })

        Level.GROUPS -> copy(divisions = divisions.map { d -> d.copy(groups = d.groups.map { it.copy(teams = emptyList()) }) })

        Level.TEAMS -> copy(divisions = divisions.map { d ->
            d.copy(groups = d.groups.map { g ->
                g.copy(teams = g.teams.map { it.copy(people = emptyList()) })
            })
        })
    }

    /** Built leaves-first: the containers hold their children in `val`s, so a layer cannot exist before the one below it. */
    private fun generate(key: Dataset): Company = Company(
        key,
        with(Person.pool) {
            List(CorporateDivision.DIVISIONS.size * key.branchCountGroups * key.branchCountTeams * key.branchCountPeople) {
                get(it % size).copy(id = it)
            }
        }
            .asSequence()
            .chunked(key.branchCountPeople).mapIndexed(ProductTeam::of)
            .chunked(key.branchCountTeams).mapIndexed(CorporateGroup::of)
            .chunked(key.branchCountGroups).mapIndexed(CorporateDivision::of)
            .toList()
    )
}
