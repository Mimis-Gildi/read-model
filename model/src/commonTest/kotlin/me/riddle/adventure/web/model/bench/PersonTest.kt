/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: $REFACTORED%.
 * The remainder is validated prototyping slop,
 *   provisionally accepted and temporary.
 */

package me.riddle.adventure.web.model.bench

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonTest {

    val tLog = KotlinLogging.logger {}

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

    companion object {
        const val RANDOM_IMPL = "The random library version may have changed"
    }
}
