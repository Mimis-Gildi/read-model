/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

@file:JsExport

package me.riddle.adventure.web.model.bench

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
data class CorporateDivision(
    val id: Int,
    val division: String,
    val groups: List<CorporateGroup>
) {
    companion object {

        fun of(id: Int, corporateGroups: List<CorporateGroup>) =
            CorporateDivision(id, DIVISIONS[id % DIVISIONS.size], corporateGroups)

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

        val DIVISIONS = DIVISIONS_ALL.slice(0..12)

    }
}

@Serializable
data class CorporateGroup(
    val id: Int,
    val group: String,
    val teams: List<ProductTeam>
) {
    companion object {

        fun of(id: Int, productTeams: List<ProductTeam>) =
            CorporateGroup(id, "${GROUPS[id % GROUPS.size]} $id", productTeams)

        val GROUPS = arrayOf(
            "Platform", "Core Services", "Developer Tools", "Identity", "Payments", "Search",
            "Storage", "Networking", "Observability", "Mobile", "Web", "API",
            "Machine Learning", "Analytics", "Compliance", "Growth", "Billing", "Integrations",
            "Partnerships", "Enablement",
        )
    }
}

@Serializable
data class ProductTeam(
    val id: Int,
    val team: String,
    val people: List<Person>
) {
    companion object {

        fun of(id: Int, teamMembers: List<Person>) =
            ProductTeam(id, "${TEAMS[id % TEAMS.size]} $id", teamMembers)

        val TEAMS = arrayOf(
            "Atlas", "Beacon", "Cascade", "Delta", "Ember", "Foundry", "Gateway", "Harbor",
            "Ironwood", "Juniper", "Keystone", "Lantern", "Meridian", "North Star", "Orbit",
            "Pinnacle", "Quarry", "Redwood", "Summit", "Trellis", "Umbra", "Vertex",
            "Wavelength", "Zenith",
        )
    }
}
