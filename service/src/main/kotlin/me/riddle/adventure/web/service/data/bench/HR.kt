package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

/**
 * Root container is the index 0 object.
 */
@Serializable
data class CorporateDivision(
    val id: Int,
    val division: String,
    val groups: List<CorporateGroup>
) {
    companion object {

        /**
         * Name is a pure function of the tree-wide [id], same contract as [Person.of].
         * No suffix here: there are exactly as many divisions as names, so the id maps one-to-one.
         */
        fun of(id: Int, corporateGroups: List<CorporateGroup>) =
            CorporateDivision(id, DIVISIONS[id % DIVISIONS.size], corporateGroups)

        /**
         * Expand loading by division chunk. Experimentation determines the most effective master load size.
         *
         * Initial load size is 12.
         */
        val DIVISIONS_ALL = arrayOf(
            "Engineering", "Product", "Design", "Data", "Security", "Infrastructure",
            "Sales", "Marketing", "Finance", "People Operations", "Legal", "Customer Success",
            "Business Operations", "Corporate Strategy", "Supply Chain and Logistics", "Procurement and Sourcing",
            "Facilities and Real Estate", "Revenue Operations", "Business Development", "Partnerships and Alliances",
            "Enablement", "Corporate Communications","Public Relations and Media","Investor Relations",
            "Government Affairs and Public Policy", "Developer Relations and Advocacy", "Research and Development ",
            "Quality Assurance and Testing", "Localization and Globalization", "Risk Management", "Compliance and Ethics",
            "Corporate Social Responsibility", "Talent Acquisition and Recruiting", "Diversity, Equity, and Inclusion",
            "Learning and Development", "Populist Policy Countermeasures"
        )

        /**
         * Starting with twelve divisions, this is the only level a human reads at the page root,
         * and the first request renders exactly this many rows, so it is the entry point rather
         * than a measurement rung.
         *
         * The count is calculated by linear arithmetic -- with uniform branching `b` the tree
         * holds `12 * (1 + b + b^2 + b^3)` nodes, which puts every declared [Dataset] range on an
         * integer `b` (5 -> 1,872, 11 -> 17,568, 25 -> 195,312) without twelve needing change.
         */
        val DIVISIONS = DIVISIONS_ALL.slice(0..11)

    }
}

/**
 * The index 1 container object.
 */
@Serializable
data class CorporateGroup(
    val id: Int,
    val group: String,
    val teams: List<ProductTeam>
) {
    companion object {

        /** Consistent with the previous index: by [id] and same contract as [Person.of]. */
        fun of(id: Int, productTeams: List<ProductTeam>) =
            CorporateGroup(id, "${GROUPS[id % GROUPS.size]} $id", productTeams)

        /**
         * Initially sized for LOAD at `b = 25`.
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
    val people: List<Person>
) {
    companion object {

        /** Consistent with previous levels. */
        fun of(id: Int, teamMembers: List<Person>) =
            ProductTeam(id, "${TEAMS[id % TEAMS.size]} $id", teamMembers)

        /**
         * Initial loading for 7,500 on LOAD.
         */
        val TEAMS = arrayOf(
            "Atlas", "Beacon", "Cascade", "Delta", "Ember", "Foundry", "Gateway", "Harbor",
            "Ironwood", "Juniper", "Keystone", "Lantern", "Meridian", "North Star", "Orbit",
            "Pinnacle", "Quarry", "Redwood", "Summit", "Trellis", "Umbra", "Vertex",
            "Wavelength", "Zenith",
        )
    }
}
