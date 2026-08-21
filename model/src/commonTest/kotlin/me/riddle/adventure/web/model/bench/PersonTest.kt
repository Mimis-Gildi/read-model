/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

package me.riddle.adventure.web.model.bench

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PersonTest {

    val tLog = KotlinLogging.logger {}

    /**
     * Verifies that the phone number generator in the companion object of the
     * [Person] class produces the EXPECTED (`random`-locked) results for some
     * specific index-to-phone-number mappings. When one fails -- all do.
     */
    @Test
    fun `spot-test companion phone number generator`() = mapOf(
        0 to "(298) 245-1653",
        1 to "(396) 490-3306",
        49 to "(312) 258-2650",
        50 to "(410) 503-4303",
        51 to "(508) 748-5956",
        99 to "(424) 516-5300",
        100 to "(522) 761-6953",
        101 to "(621) 006-8606",
    ).forEach { (index, phoneNumber) ->
        assertEquals(phoneNumber, Person.phoneOf(index), "$RANDOM_IMPL ($index)")
        tLog.info { "PASS: Person.phoneOf($index) = $phoneNumber" }
    }

    @Test
    fun `spot-test companion person pool generator`() = assertEquals(
        listOf("Justin", "Ryan", "Samuel"),
        Person.makePool().groupBy { it.firstName }
            .map { p -> p.key to p.value.size }
            .sortedByDescending { p -> p.second }
            .filter { it.second > 4 }
            .map { p -> p.first }
            .also {
                tLog.info { "PASS:  $it" }
            },
        "$RANDOM_IMPL ([(Justin, 7), (Ryan, 5), (Samuel, 5)])"
    )

    @Test
    fun `person of index is the index, repeatably`() = listOf(0, 1, 50, 100).forEach { index ->
        assertEquals(index, Person.of(index).id, "The index is the id")
        assertEquals(Person.phoneOf(index), Person.of(index).phone, "The phone is the arithmetic one")
        assertEquals(Person.of(index), Person.of(index), "$RANDOM_IMPL ($index)")
    }

    @Test
    fun `pool is prime, lazy like my teens, and the same instance as yesterday`() {
        assertEquals(101, Person.pool.size, "The default is prime on purpose")
        assertSame(Person.pool, Person.pool, "Callers link the same pool")
        assertEquals(Person.makePool(), Person.pool, "A fresh pool is an equal pool")
    }

    @Test
    fun `quick de-and-serializer test`() = Person.of(0).let {
        assertEquals(
            it,
            Json.decodeFromString<Person>(Json.encodeToString(it).also { json -> tLog.info { "Person is: $json" } }),
            "Every field survives the wire"
        )
    }

    companion object {
        const val RANDOM_IMPL = "The random library version may have changed"
    }
}
