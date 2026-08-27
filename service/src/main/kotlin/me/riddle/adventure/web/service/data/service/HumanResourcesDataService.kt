package me.riddle.adventure.web.service.data.service

import me.riddle.adventure.web.service.data.bench.*

class HumanResourcesDataService() {

    private val sets by lazy { Dataset.entries.associateWith { generate(it) } }

    fun get(dataset: Dataset): Company = sets.getValue(dataset)

    fun get(dataset: Dataset, level: Level): Company = get(dataset).cullTo(level)

    /**
     * The level filter, applied on the way up: every layer deeper than [level] becomes an empty collection.
     * [Level] names the deepest type the response still carries, so [Level.PEOPLE] is the whole tree untouched.
     *
     * Nothing is regenerated. The surviving layers are the same instances as the full tree; only
     * the containers on the path are copied, so a projection costs one pass and no new people.
     *
     * This has a very specific purpose in UX/UI testing so that my godly Vanilla JS is benched against:
     * 1) Chrome's natural crash on data fetch with and without Pretty-print - pure C++ magic by Google;
     * 2) And against React and other animals in this zoo on equal footing.
     *
     * Bench results are going into the associated article.
     */
    private fun Company.cullTo(level: Level): Company = when (level) {
        Level.DIVISIONS -> copy(divisions = divisions.map { it.copy(groups = emptyList()) })
        Level.GROUPS -> copy(divisions = divisions.map { d -> d.copy(groups = d.groups.map { it.copy(teams = emptyList()) }) })
        Level.TEAMS -> copy(divisions = divisions.map { d ->
            d.copy(groups = d.groups.map { g -> g.copy(teams = g.teams.map { it.copy(people = emptyList()) }) })
        })

        Level.PEOPLE -> this
    }

    /**
     * Built leaves-first: the containers hold their children in `val`s, so a layer cannot exist
     * before the one below it. Each layer is exactly [Dataset.branching] times the size of the one
     * above, so [chunked] always divides evenly and ids stay dense and sequential per layer.
     */
    private fun generate(key: Dataset): Company = Company(
        key,
        with(Person.pool) {
            List(CorporateDivision.DIVISIONS.size * key.branching * key.branching * key.branching) {
                get(it % size).copy(id = it)
            }
        }
            .asSequence()
            .chunked(key.branching).mapIndexed(ProductTeam::of)
            .chunked(key.branching).mapIndexed(CorporateGroup::of)
            .chunked(key.branching).mapIndexed(CorporateDivision::of)
            .toList()
    )
}
