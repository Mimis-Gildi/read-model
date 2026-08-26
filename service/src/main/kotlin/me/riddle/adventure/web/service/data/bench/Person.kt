package me.riddle.adventure.web.service.data.bench

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Index 3 is the last leaf object.
 */
@Serializable
data class Person(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val jobTitle: String,
    val location: String,
    val phone: String
) {
    companion object {

        val pool by lazy { pool() }

        /**
         * A reusable pool. Callers link the same instance from many [ProductTeam]s and give it a tree-unique id
         * with `copy(id = ...)`.
         *
         * Deterministic by construction: every field is a pure function of the pool index, so the same pool
         * comes out byte-identical on every run and in every module.
         *
         * The default is prime on purpose: it shares no factor with any branching factor, so team
         * rosters cannot fall into a repeating cycle. At 100 with `b = 25`, every fourth team drew
         * an identical roster under different ids.
         */
        fun pool(size: Int = 101): List<Person> = List(size, ::of)

        /**
         * The whole person predictably derived from [index].
         * No hidden RNG state, no clock, no I/O.
         */
        fun of(index: Int): Person {
            val random = Random(index.toLong())
            return Person(
                id = index,
                firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.size)],
                lastName = LAST_NAMES[random.nextInt(LAST_NAMES.size)],
                jobTitle = JOB_TITLES[random.nextInt(JOB_TITLES.size)],
                location = LOCATIONS[random.nextInt(LOCATIONS.size)],
                phone = phoneOf(index)
            )
        }

        /**
         * Ten digits straight out of [index], then formatted.
         *
         * Deliberately arithmetic rather than drawn from [Random]: the seeded generator is only
         * repeatable "within the same version of Kotlin runtime", and `String.format` without an
         * explicit locale emits localized digits under a non-ASCII numbering system. Plain
         * multiply-and-modulo has neither problem, and it does not shift when fields are reordered.
         *
         * A large prime multiplier spreads consecutive indices across the range; the 2_000_000_000
         * floor keeps the value at exactly ten digits, so the slices below always line up.
         */
        fun phoneOf(index: Int) = ((index + 1) * 982_451_653L % 8_000_000_000L + 2_000_000_000L)
            .toString().run { "(${substring(0, 3)}) ${substring(3, 6)}-${substring(6)}" }


        /**
         * Common US first names, 50 male and 50 female.
         * A realistic length distribution with plausible text.
         */
        private val FIRST_NAMES = arrayOf(
            "James", "Robert", "John", "Michael", "David", "William", "Richard", "Joseph",
            "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald",
            "Steven", "Andrew", "Paul", "Joshua", "Kenneth", "Kevin", "Brian", "George",
            "Timothy", "Ronald", "Jason", "Edward", "Jeffrey", "Ryan", "Jacob", "Gary",
            "Nicholas", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Scott", "Brandon",
            "Benjamin", "Samuel", "Gregory", "Alexander", "Patrick", "Frank", "Raymond", "Jack",
            "Dennis", "Jerry",
            "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica",
            "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Sandra", "Margaret", "Ashley",
            "Kimberly", "Emily", "Donna", "Michelle", "Carol", "Amanda", "Melissa", "Deborah",
            "Stephanie", "Rebecca", "Laura", "Sharon", "Cynthia", "Kathleen", "Amy", "Angela",
            "Shirley", "Anna", "Brenda", "Pamela", "Emma", "Nicole", "Helen", "Samantha",
            "Katherine", "Christine", "Debra", "Rachel", "Carolyn", "Janet", "Maria", "Olivia",
            "Heather", "Diane",
        )

        /**
         * Common US last names, 100 for entropy.
         */
        private val LAST_NAMES = arrayOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
            "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
            "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
            "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
            "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
            "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy",
            "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper", "Peterson", "Bailey",
            "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson",
            "Watson", "Brooks", "Chavez", "Wood", "James", "Bennett", "Gray", "Mendoza",
            "Ruiz", "Hughes", "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers",
            "Long", "Ross", "Foster", "Jimenez",
        )

        /**
         * An array of commonly used job titles.
         *
         * This collection is used to deterministically assign random job titles to instances of Person.
         */
        private val JOB_TITLES = arrayOf(
            "Software Engineer", "Senior Software Engineer", "Staff Engineer", "Product Manager",
            "Engineering Manager", "Data Analyst", "Data Scientist", "QA Engineer",
            "Site Reliability Engineer", "Technical Writer", "UX Designer", "Solutions Architect",
            "Database Administrator", "Security Analyst", "Business Analyst", "Scrum Master",
            "Account Executive", "Support Specialist", "Recruiter", "Financial Analyst",
        )

        /**
         * An array of commonly used locations.
         *
         * This collection is used to deterministically assign random locations to instances of Person.
         */
        private val LOCATIONS = arrayOf(
            "New York, NY", "San Francisco, CA", "Seattle, WA", "Austin, TX", "Chicago, IL",
            "Boston, MA", "Denver, CO", "Atlanta, GA", "Portland, OR", "Miami, FL",
            "Pittsburgh, PA", "Raleigh, NC", "Phoenix, AZ", "Minneapolis, MN", "Remote",
        )
    }
}
