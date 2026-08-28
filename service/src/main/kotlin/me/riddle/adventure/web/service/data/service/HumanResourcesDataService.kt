package me.riddle.adventure.web.service.data.service

import me.riddle.adventure.web.service.data.bench.*

class HumanResourcesDataService {

    private val sets by lazy { Dataset.entries.associateWith { generate(it) } }

    fun get(dataset: Dataset): Company = sets.getValue(dataset)

    fun get(dataset: Dataset, level: Level): Company = get(dataset).cullTo(level)

    /**
     * [Level] names the deepest type the response still carries; everything below it comes back empty.
     * Nothing is regenerated -- the surviving layers are the same instances, only the containers on the path are copied.
     *
     * This has a very specific purpose in UX/UI testing so that my godly Vanilla JS is benched against:
     * 1) Chrome's natural crash on data fetch with and without Pretty-print - pure C++ magic by Google;
     * 2) And against React and other animals in this zoo on equal footing.
     *
     * Bench results are going into the associated article.
     */
    private fun Company.cullTo(level: Level): Company = when (level) {
        Level.DIVISIONS -> copy(divisions = divisions.map { it.copy(groups = emptyList()) })

        Level.GROUPS -> copy(divisions = divisions.map { d ->
            d.copy(groups = d.groups.map { it.copy(teams = emptyList()) })
        })

        Level.TEAMS -> copy(divisions = divisions.map { d ->
            d.copy(groups = d.groups.map { g ->
                g.copy(teams = g.teams.map { it.copy(people = emptyList()) })
            })
        })

        Level.PEOPLE -> this
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
