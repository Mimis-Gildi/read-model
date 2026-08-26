package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val dataset: Dataset,
    val divisions: List<CorporateDivision>
)

/**
 * The three dataset sizes.
 *
 * [minSize] and [maxSize] bound what the control plane may offer; [branching] is the size we
 * actually start from and the one the tests are written against. Every level below the divisions
 * fans out by [branching], so a dataset holds `12 * (1 + b + b^2 + b^3)` nodes and the whole shape
 * follows from a single number.
 *
 * [key] is shown on the statistics column.
 *
 * Divisions are fixed at twelve -- see [CorporateDivision.DIVISIONS] for why.
 */
@Serializable
enum class Dataset(
    val key: String,
    val minSize: Int,
    val maxSize: Int,
    val branching: Int,
) {
    /**
     * Divisions=12;
     * 48: groups per division=4,
     * 192: teams per group=4,
     * 768: people per team=4 -- 1,020 nodes. */
    SMOKE("smoke", 100, 2_000, 4),

    /**
     * Divisions=12;
     * 132: groups per division=11,
     * 1,452: teams per group=11,
     * 15,972: people per team=11 -- 17,568 nodes. */
    BENCH("bench", 1_000, 20_000, 11),

    /**
     * Divisions=12;
     * 300: groups per division=25,
     * 7,500: teams per group=25,
     * 187,500: people per team=25 -- 195,312 nodes. */
    LOAD("load", 10_000, 200_000, 25);
}
