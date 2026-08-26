package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

/**
 * Root container is the index 0 object.
 */
@Serializable
data class CorporateDivision(
    val id: Int,
    val division: String,
    val groups: List<CorporateGroup>?
) {
    companion object {

        /**
         * Name is a pure function of the tree-wide [id], same contract as [Person.of].
         * No suffix here: there are exactly as many divisions as names, so the id maps one-to-one.
         */
        fun of(id: Int, corporateGroups: List<CorporateGroup>?) =
            CorporateDivision(id, DIVISIONS[id % DIVISIONS.size], corporateGroups)

        /**
         * The twelve divisions. Fixed and small on purpose: this is the only level a human reads,
         * and the first request renders exactly this many rows, so it is the entry point rather
         * than a measurement rung.
         *
         * The count is load-bearing on the shape arithmetic -- with uniform branching `b` the tree
         * holds `12 * (1 + b + b^2 + b^3)` nodes, which puts every declared [Dataset] range on an
         * integer `b` (5 -> 1,872, 11 -> 17,568, 25 -> 195,312) without twelve needing change.
         */
        val DIVISIONS = arrayOf(
            "Engineering", "Product", "Design", "Data", "Security", "Infrastructure",
            "Sales", "Marketing", "Finance", "People Operations", "Legal", "Customer Success",
        )
    }
}

/**
 * The index 1 container object.
 */
@Serializable
data class CorporateGroup(
    val id: Int,
    val group: String,
    val teams: List<ProductTeam>?
) {
    companion object {

        /** Name is a pure function of the tree-wide [id], same contract as [Person.of]. */
        fun of(id: Int, productTeams: List<ProductTeam>?) =
            CorporateGroup(id, "${GROUPS[id % GROUPS.size]} $id", productTeams)

        /**
         * Twenty group names. The peak group count is 300 (LOAD at `b = 25`), so the suffix -- not
         * this array -- is what makes a name unique; the vocabulary only buys variety, at roughly
         * fifteen reuses each. Three hundred distinct names would buy nothing.
         */
        val GROUPS = arrayOf(
            "Platform", "Core Services", "Developer Tools", "Identity", "Payments", "Search",
            "Storage", "Networking", "Observability", "Mobile", "Web", "API",
            "Machine Learning", "Analytics", "Compliance", "Growth", "Billing", "Integrations",
            "Partnerships", "Enablement",
        )
    }
}

/**
 * Index 2 is the last container object.
 */
@Serializable
data class ProductTeam(
    val id: Int,
    val team: String,
    val people: List<Person>?
) {
    companion object {

        /** Name is a pure function of the tree-wide [id], same contract as [Person.of]. */
        fun of(id: Int, teamMembers: List<Person>?) =
            ProductTeam(id, "${TEAMS[id % TEAMS.size]} $id", teamMembers)

        /**
         * Twenty-four team names, single short tokens on purpose: this is the widest container
         * level (7,500 at LOAD), so keeping the rendered row width uniform here keeps layout cost
         * comparable between levels rather than confounded by string length.
         */
        val TEAMS = arrayOf(
            "Atlas", "Beacon", "Cascade", "Delta", "Ember", "Foundry", "Gateway", "Harbor",
            "Ironwood", "Juniper", "Keystone", "Lantern", "Meridian", "North Star", "Orbit",
            "Pinnacle", "Quarry", "Redwood", "Summit", "Trellis", "Umbra", "Vertex",
            "Wavelength", "Zenith",
        )
    }
}
