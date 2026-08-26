package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

@Serializable
data class Bench(
    val name: Dataset,
    val data: List<CorporateDivision>
)

@Serializable
enum class Dataset(val key: String, val minSize: Int, val maxSize: Int) {
    SMOKE("smoke", 100, 2_000),
    BENCH("bench", 1_000, 20_000),
    LOAD("load", 10_000, 200_000);
}
