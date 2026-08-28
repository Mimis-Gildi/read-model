package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val dataset: Dataset,
    val divisions: List<CorporateDivision>
)

@Serializable
enum class Dataset(
    val key: String,
    val branchCountGroups: Int,
    val branchCountTeams: Int,
    val branchCountPeople: Int
) {
    SMOKE("smoke", 7, 4, 4),
    BENCH("bench", 11, 11, 11),
    LOAD("load", 25, 25, 25),
    FAIL("boom", 45, 35, 25);

    val nodes: Int
        get() = CorporateDivision.DIVISIONS.size *
                (1 + branchCountGroups + branchCountGroups * branchCountTeams + branchCountGroups * branchCountTeams * branchCountPeople)

    companion object {

        /** Resolves the [key] carried on the URL, so the wire never sees the constant name. */
        fun of(key: String?) = entries.find { it.key == key }
    }
}
