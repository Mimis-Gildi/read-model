/*
 * `harness/model.js`, in Kotlin.
 *
 * CAUTION on `dynamic`: the tree is walked as parsed JSON on purpose. Deserializing into data classes would put a
 * per-node allocation between the fetch and the render that no other module pays, which is a different experiment.
 */
package me.riddle.adventure.web.kobweb.bench

import me.riddle.adventure.web.model.ICON_LEAF
import me.riddle.adventure.web.model.ICON_OPEN
import me.riddle.adventure.web.model.ICON_SHUT

const val OPEN = ICON_OPEN
const val SHUT = ICON_SHUT
const val LEAF = ICON_LEAF

class Level(
    val children: String,
    val label: (dynamic) -> String,
    val meta: (dynamic, Int) -> String = { _, kids -> count(kids) },
    val collapsed: Boolean = false,
)

val LEVELS = listOf(
    Level("groups", { it.division as String }),
    Level("teams", { it.group as String }),

    // Teams ship folded!
    Level("people", { it.team as String }, collapsed = true),
    Level(
        children = "",
        label = { "${it.firstName as String} ${it.lastName as String}" },
        meta = { person, _ ->
            "${(person.jobTitle as String).padEnd(26)}${(person.location as String).padEnd(18)}${person.phone as String}"
        },
    ),
)

fun count(n: Int): String = n.asDynamic().toLocaleString() as String

fun kids(node: dynamic, depth: Int): Array<dynamic> = LEVELS[depth].children
    .let { field -> if (field.isEmpty()) emptyArray() else node[field].unsafeCast<Array<dynamic>>() }

/**
 * [elements] is derived rather than counted during the build, which would put an increment inside the clock:
 * five per node -- the box, the row and its three spans -- plus the one `.kids` container a node with children needs.
 */
class Census(val nodes: Int, val elements: Int) {
    operator fun plus(other: Census) = Census(nodes + other.nodes, elements + other.elements)
}

// CAUTION: a culled level still carries its children field, present but empty. Keying the sixth element on the field
// name rather than on the array charges every leaf for a `.kids` div that `pure.js` never creates.
private fun tally(node: dynamic, depth: Int): Census = kids(node, depth).let { children ->
    children.fold(Census(1, if (children.isEmpty()) 5 else 6)) { sum, child -> sum + tally(child, depth + 1) }
}

fun census(company: dynamic): Census = company.divisions.unsafeCast<Array<dynamic>>()
    .fold(Census(0, 0)) { sum, division -> sum + tally(division, 0) }
