package me.riddle.adventure.web.service.data.service

import me.riddle.adventure.web.service.data.bench.Bench
import me.riddle.adventure.web.service.data.bench.Dataset

class BenchDataService(sizes: Map<Dataset, Int>) {
    private val sets: Map<Dataset, Bench> = sizes.mapValues { generate(it.key, it.value) }
    fun get(dataset: Dataset): Bench = sets.getValue(dataset)

    private fun generate(key: Dataset, value: Int): Bench {
        TODO("Not yet implemented")
    }
}
