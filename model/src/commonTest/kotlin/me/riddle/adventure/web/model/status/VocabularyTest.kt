/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.model.status

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import kotlin.test.*

class VocabularyTest {

    fun DatasetOption.asString() = "$key ($label) $nodes"
    fun FrameworkOption.asString() = "$key ($label)"

    fun datasetAsString(ds: DatasetOption) = ds.asString()
    fun moduleAsString(mod: FrameworkOption) = mod.asString()

    companion object {
        val tLogger by lazy { KotlinLogging.logger {} }
        val vocabulary by lazy {
            tLogger.trace { "Creating test vocabulary cuz asked." }
            Vocabulary(
                frameworks = listOf(
                    FrameworkOption(key = "node", label = "Bare Node Test"),
                    FrameworkOption(key = "vue-core", label = "VueJS Minimal"),
                    FrameworkOption(key = "vue-full", label = "VueJS Full App"),
                    FrameworkOption(key = "react-core", label = "React minimal"),
                    FrameworkOption(key = "react-full", label = "React Full App"),
                    FrameworkOption(key = "angular-core", label = "Angular Minimal"),
                    FrameworkOption(key = "angular-full", label = "Angular Full App"),
                    FrameworkOption(key = "svelte", label = "Svelte Full App"),
                    FrameworkOption(key = "elm", label = "Elegant App"),
                    FrameworkOption(key = "purescript", label = "Laggard don't Monad")
                ),
                datasets = listOf(
                    DatasetOption(key = "minimal", label = "Smallest test size", nodes = 800),
                    DatasetOption(key = "smoke", label = "Smallest practical test size", nodes = 2200),
                    DatasetOption(key = "medium", label = "Medium test size", nodes = 5400),
                    DatasetOption(key = "load", label = "Load testing size", nodes = 11200),
                    DatasetOption(key = "large", label = "Large test size", nodes = 19700),
                    DatasetOption(key = "ceiling", label = "Performance ceiling data size", nodes = 22400),
                    DatasetOption(key = "torture", label = "Torture test size", nodes = 31000),
                    DatasetOption(key = "crash", label = "Crash test size", nodes = 50000)
                )
            )
        }
        val expectedFrameworks by lazy {
            tLogger.trace { "Creating expected frameworks list cuz asked" }
            listOf(
                "node (Bare Node Test)",
                "vue-core (VueJS Minimal)",
                "vue-full (VueJS Full App)",
                "react-core (React minimal)",
                "react-full (React Full App)",
                "angular-core (Angular Minimal)",
                "angular-full (Angular Full App)",
                "svelte (Svelte Full App)",
                "elm (Elegant App)",
                "purescript (Laggard don't Monad)"
            )
        }
        val expectedDatasets by lazy {
            tLogger.trace { "Creating expected datasets list cuz asked" }
            listOf(
                "minimal (Smallest test size) 800",
                "smoke (Smallest practical test size) 2200",
                "medium (Medium test size) 5400",
                "load (Load testing size) 11200",
                "large (Large test size) 19700",
                "ceiling (Performance ceiling data size) 22400",
                "torture (Torture test size) 31000",
                "crash (Crash test size) 50000"
            )
        }

        val vocabOnTheWire by lazy {
            tLogger.trace { "Somebody asked for pretty JSON" }
            """
                {
                  "type": "One Pretty Vocabulary",
                  "frameworks": [
                    {
                      "key": "kotlin-html",
                      "label": "Pure HTML in Kotlin"
                    },
                    {
                      "key": "compose",
                      "label": "Compose HTML"
                    }
                  ],
                  "datasets": [
                    {
                      "key": "big",
                      "label": "Big Test!",
                      "nodes": 80000
                    },
                    {
                      "key": "small",
                      "label": "Small Test",
                      "nodes": 2
                    }
                  ]
                }
            """.trimIndent()
        }

        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }


    @Test
    fun `should serialize and deserialize correct vocabulary`() {
        tLogger.trace { "Vocabulary is:\n\n${prettyJson.encodeToString(vocabulary)}" }

        val vocOnWire = Json.encodeToString<Frame>(vocabulary)
        assertNotEquals("", vocOnWire, "Encoded vocabulary is a non-empty string")

        val vocOnDisk = Json.parseToJsonElement(vocOnWire)
        val typeVal = vocOnDisk.jsonObject["type"]?.jsonPrimitive?.content
        val frameworksVal = vocOnDisk.jsonObject["frameworks"]?.jsonArray
        val datasetsVal = vocOnDisk.jsonObject["datasets"]?.jsonArray

        assertNotNull(typeVal, "Type value is not null: kotlinx.serialization writes the discriminator")
        assertNotNull(frameworksVal, "Frameworks value is not null as it's an array of 10 objects")
        assertNotNull(datasetsVal, "Datasets value is not null as it's an array of 8 objects")

        assertEquals("vocabulary", typeVal, "Discriminator value is 'vocabulary', from @SerialName")
        assertEquals(10, frameworksVal.size, "Frameworks value is an array of 10 objects")
        assertEquals(8, datasetsVal.size, "Datasets value is an array of 8 objects")

        val frameworks = Json.decodeFromJsonElement<List<FrameworkOption>>(frameworksVal)
        assertNotNull(frameworks, "Frameworks value is not null because it's made of an array of 10 objects")
        assertContentEquals(
            expectedFrameworks, frameworks.map(::moduleAsString),
            "All ten test frameworks are present as strings"
        )

        val datasets = Json.decodeFromJsonElement<List<DatasetOption>>(datasetsVal)
        assertNotNull(datasets, "Datasets value is not null because it's made of an array of 8 objects")
        assertContentEquals(
            expectedDatasets, datasets.map(::datasetAsString),
            "All eight test datasets are present as strings"
        )

        tLogger.info { "Vocabulary passed from object." }
    }

    @Test
    fun `should deserialize to vocabulary pretty hand-json too`(){
        val localJson = Json { ignoreUnknownKeys = true }

        val prettyVocabulary = localJson.decodeFromString<Vocabulary>(vocabOnTheWire)
        tLogger.trace { "Vocabulary deserializes as $prettyVocabulary" }

        assertNotNull(prettyVocabulary)

        assertEquals("kotlin-html (Pure HTML in Kotlin)", prettyVocabulary.frameworks.first { it.key == "kotlin-html" }.asString())
        assertEquals("compose (Compose HTML)", prettyVocabulary.frameworks.first { it.key == "compose" }.asString())

        assertContentEquals( listOf("big (Big Test!) 80000", "small (Small Test) 2"), prettyVocabulary.datasets.map(::datasetAsString))


        tLogger.info { "Vocabulary passed to object." }
    }
}
