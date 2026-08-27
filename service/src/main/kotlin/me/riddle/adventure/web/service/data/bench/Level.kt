package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable

/**
 * The four layers of the read model, named after the type they carry rather than the depth they sit at.
 *
 * The names were literals in four places -- the par matrix, the fixture's results table, the fixture's tree walk, and
 * the prose of every `level` parameter -- and the depths were bare integers guarded by a `0..3` written out by hand.
 * Same cure as [Dataset] and `Module`: one enum, and the copies cannot drift apart.
 *
 * [depth] is what travels on the URL. It names the deepest type a response still carries,
 * so a request for [TEAMS] is a tree of divisions, groups, and teams with no people under them.
 */
@Serializable
enum class Level(val label: String) {
    DIVISIONS("Divisions"),
    GROUPS("Groups"),
    TEAMS("Teams"),
    PEOPLE("People");

    val depth: Int get() = ordinal

    companion object {

        /**
         * Resolves the depth carried on the URL. A depth no layer sits at resolves to nothing, which the route 404s --
         * deliberately, rather than defaulting to the whole tree: a typo'd level that quietly measures a different
         * tree is the one failure this rig cannot afford, because its output is nothing but timings.
         */
        fun at(depth: Int) = entries.getOrNull(depth)
    }
}
