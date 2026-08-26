package me.riddle.adventure.web.service.data.service

import me.riddle.adventure.web.service.data.bench.Company
import me.riddle.adventure.web.service.data.bench.Dataset

class HumanResourcesDataService(sizes: Map<Dataset, Int>) {
    private val sets: Map<Dataset, Company> = sizes.mapValues { generate(it.key, it.value) }
    fun get(dataset: Dataset): Company = sets.getValue(dataset)

    private fun generate(key: Dataset, value: Int): Company {
        TODO("Not yet implemented")
    }
}
