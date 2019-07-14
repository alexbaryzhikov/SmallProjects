/*
The following version of the puzzle appeared in Life International in 1962:

There are five houses.
The Englishman lives in the red house.
The Spaniard owns the dog.
Coffee is drunk in the green house.
The Ukrainian drinks tea.
The green house is immediately to the right of the ivory house.
The Old Gold smoker owns snails.
Kools are smoked in the yellow house.
Milk is drunk in the middle house.
The Norwegian lives in the first house.
The man who smokes Chesterfields lives in the house next to the man with the fox.
Kools are smoked in the house next to the house where the horse is kept.
The Lucky Strike smoker drinks orange juice.
The Japanese smokes Parliaments.
The Norwegian lives next to the blue house.
Now, who drinks water? Who owns the zebra?

In the interest of clarity, it must be added that each of the five houses is
painted a different color, and their inhabitants are of different national
extractions, own different pets, drink different beverages and smoke different
brands of American cigarets [sic]. One other thing: in statement 6, right means
your right.
*/

package various

import various.Cigarettes.*
import various.Colors.*
import various.Drinks.*
import various.Merger.Constraint
import various.Merger.Relation
import various.Merger.Entry.*
import various.Merger.Rule
import various.Nations.*
import various.Pets.*
import various.Relations.imRight
import various.Relations.nextTo
import java.util.*

class Merger {

    val constraints = ArrayList<Constraint>()

    fun add(a: Constraint) {
        constraints += a
    }

    fun merge(): Merger {
        addReciprocalRelations()
        var i = 1
        val modified = arrayOf(true, true, true)
        while (modified.any { it }) {
            println("=== Merge cycle ${i++} ===\n")
            modified[0] = hardMerge()
            modified[1] = resolveRules()
            modified[2] = softMerge()
        }
        return this
    }

    private fun addReciprocalRelations() {
        for (constraint in constraints) {
            for ((i, entry) in constraint.entries.withIndex()) {
                if (entry is RuleSet) {
                    for (rule in entry.rules) {
                        val reciprocalRelation = Relation(rule.relation.g, rule.relation.f)
                        val newRule = Rule(reciprocalRelation, constraint.id)
                        val otherConstraint = constraints.find { it.id == rule.id }
                                ?: throw IllegalStateException("Id ${rule.id} not found")
                        when (val otherEntry = otherConstraint.entries[i]) {
                            is None -> otherConstraint.entries[i] = RuleSet(hashSetOf(newRule))
                            is RuleSet -> otherEntry.rules.add(newRule)
                        }
                    }
                }
            }
        }
    }

    private fun hardMerge(): Boolean {
        println("> Hard merge\n")
        var modified = false
        var i = 0
        var cc = ArrayList(constraints)
        while (i < cc.lastIndex) {
            val a = cc[i]
            for (j in i + 1 until cc.size) {
                val b = cc[j]
                if (a.hardMatch(b)) {
                    merge(a, b)
                    modified = true
                }
            }
            cc = ArrayList(constraints)
            i++
        }
        return modified
    }

    private fun softMerge(): Boolean {
        println("> Soft merge\n")
        var modified = false
        var i = 0
        while (i < constraints.size) {
            val a = constraints[i]
            var b: Constraint? = null
            for (j in 0 until constraints.size) {
                val c = constraints[j]
                if (c === a) continue
                if (a.softMatch(c, constraints)) {
                    println("Found match: ${a.show()} and ${c.show()}")
                    if (b == null) {
                        b = c
                    } else {
                        b = null
                        break
                    }
                } else {
                    println("No match: ${a.show()} and ${c.show()}")
                }
            }
            if (b != null && a.id < b.id) {
                merge(a, b)
                modified = true
            }
            println()
            i++
        }
        return modified
    }

    private fun merge(a: Constraint, b: Constraint) {
        println("${a.show()} + ${b.show()}")
        a.merge(b, constraints)
        constraints.remove(b)
        updateRules(a.id, b.id)
        println("    = ${a.show()}\n")
    }

    private fun resolveRules(): Boolean {
        println("> Resolve rules\n")
        var modified = false
        var done = false
        while (!done) {
            done = true
            for (c in constraints) {
                for (i in c.entries.indices) {
                    val e = c.entries[i]
                    if (e is RuleSet) {
                        println("Resolve entry $i of ${c.show()}")
                        val v = c.resolveRuleSet(e, i, constraints)
                        if (v != null) {
                            println("Entry $i of ${c.show()} = ${v.v}")
                            c.entries[i] = v
                            done = false
                            modified = true
                        }
                    }
                }
                if (!done) break
            }
        }
        println()
        return modified
    }

    private fun updateRules(newId: Int, oldId: Int) {
        for (c in constraints) {
            for (e in c.entries) {
                if (e is RuleSet) e.rules.forEach { it.updateId(newId, oldId) }
            }
        }
    }

    private fun Rule.updateId(newId: Int, oldId: Int) {
        if (id == oldId) id = newId
    }

    class Constraint(val id: Int, vararg entries: Entry) {

        val entries =
                if (entries.size == CONSTRAINT_TYPES) arrayOf(*entries)
                else throw IllegalArgumentException("Wrong number of arguments")

        fun resolveRuleSet(e: RuleSet, i: Int, constraints: List<Constraint>): Value? {
            val values = e.possibleValues(i, constraints)
            return when(values.size) {
                0 -> throw IllegalStateException("Constraints can't be satisfied")
                1 -> Value(values.first())
                else -> null
            }
        }

        fun softMatch(other: Constraint, constraints: List<Constraint>): Boolean {
            for (i in entries.indices) {
                val a = entries[i]
                val b = other.entries[i]
                when (a) {
                    is Value -> when (b) {
                        is Value -> return false
                        is RuleSet -> if (a.v !in b.possibleValues(i, constraints, a.v)) return false
                    }
                    is RuleSet -> when (b) {
                        is Value -> if (b.v !in a.possibleValues(i, constraints, b.v)) return false
                        is RuleSet -> {
                            // If one depends on other -- can't merge.
                            if (id in b.rules.map { it.id } || other.id in a.rules.map { it.id }) return false
                            // No common values -- can't merge.
                            val valuesA = a.possibleValues(i, constraints)
                            val valuesB = b.possibleValues(i, constraints)
                            if (valuesA.intersect(valuesB).isEmpty()) return false
                        }
                    }
                }
            }
            return true
        }

        fun hardMatch(other: Constraint): Boolean {
            for (i in entries.indices) {
                val entry = entries[i]
                val otherEntry = other.entries[i]
                if (entry is Value && otherEntry is Value && entry.v == otherEntry.v) return true
            }
            return false
        }

        fun merge(other: Constraint, constraints: List<Constraint>) {
            for (i in entries.indices) {
                entries[i] = when (val entry = entries[i]) {
                    is None -> other.entries[i]
                    is Value -> entry
                    is RuleSet -> when (val otherEntry = other.entries[i]) {
                        is None -> entry
                        is Value -> otherEntry
                        is RuleSet -> entry.merge(otherEntry, i, constraints)
                    }
                }
            }
        }

        private fun RuleSet.merge(other: RuleSet, i: Int, constraints: List<Constraint>): Entry {
            rules.addAll(other.rules)
            val result = possibleValues(i, constraints)
            return when(result.size) {
                0 -> throw IllegalStateException("Constraints can't be satisfied")
                1 -> Value(result.first())
                else -> this
            }
        }

        private fun RuleSet.possibleValues(
                i: Int,
                constraints: List<Constraint>,
                retainedValue: Int? = null
        ): Set<Int> {
            val values: HashSet<Int> = availableValues(i, constraints)
            if (retainedValue != null) values.add(retainedValue)
            for (rule in rules) {
                values.retainAll(rule.possibleValues(constraints))
            }
            return values
        }

        private fun Rule.possibleValues(constraints: List<Constraint>): Set<Int> {
            val c = constraints.find { it.id == id } ?: throw IllegalStateException("Id $id not found")
            return relation.f(c)
        }

        private fun availableValues(i: Int, constraints: List<Constraint>): HashSet<Int> {
            val values: HashSet<Int> = HashSet(List(CONSTRAINT_VARIANTS) { it })
            for (c in constraints) {
                val e = c.entries[i]
                if (e is Value) {
                    values.remove(e.v)
                }
            }
            return values
        }
    }

    sealed class Entry {
        object None : Entry()
        class Value(val v: Int) : Entry()
        class RuleSet(val rules: HashSet<Rule>) : Entry()
    }

    data class Rule(var relation: Relation, var id: Int)

    data class Relation(
            val f: (Constraint) -> Set<Int>, // relation function
            val g: (Constraint) -> Set<Int>  // reciprocal of f
    )

    companion object {
        const val CONSTRAINT_TYPES = 6
        const val CONSTRAINT_VARIANTS = 5
    }
}

enum class Colors { RED, GREEN, IVORY, YELLOW, BLUE }
enum class Nations { ENGLISHMAN, SPANIARD, UKRAINIAN, JAPANESE, NORWEGIAN }
enum class Pets { DOG, SNAILS, FOX, HORSE, ZEBRA }
enum class Drinks { COFFEE, TEA, MILK, ORANGE_JUICE, WATER }
enum class Cigarettes { OLD_GOLD, KOOLS, CHESTERFIELDS, LUCKY_STRIKE, PARLIAMENTS }

fun Constraint.show(): String {
    val pos = if (entries[0] is Value) (entries[0] as Value).v.toString() else "?"
    val col = if (entries[1] is Value) Colors.values()[(entries[1] as Value).v].name else "?"
    val nat = if (entries[2] is Value) Nations.values()[(entries[2] as Value).v].name else "?"
    val pet = if (entries[3] is Value) Pets.values()[(entries[3] as Value).v].name else "?"
    val dri = if (entries[4] is Value) Drinks.values()[(entries[4] as Value).v].name else "?"
    val cig = if (entries[5] is Value) Cigarettes.values()[(entries[5] as Value).v].name else "?"
    return "[$pos, $col, $nat, $pet, $dri, $cig]"
}

object Relations {

    private val variants: Set<Int> = HashSet(List(5) { it })

    val imRight = Relation(::imRightF, ::imLeftF)
    val nextTo = Relation(::nextToF, ::nextToF)

    private fun imRightF(c: Constraint): Set<Int> {
        val p = c.entries[0]
        return if (p is Value) {
            variants.intersect(setOf(p.v + 1))
        } else {
            variants.subtract(setOf(0))
        }
    }

    private fun imLeftF(c: Constraint): Set<Int> {
        val p = c.entries[0]
        return if (p is Value) {
            variants.intersect(setOf(p.v - 1))
        } else {
            variants.subtract(setOf(4))
        }
    }

    private fun nextToF(c: Constraint): Set<Int> {
        val p = c.entries[0]
        return if (p is Value) {
            variants.intersect(setOf(p.v - 1, p.v + 1))
        } else {
            variants
        }
    }
}

fun <T : Enum<T>> value(v: T) = Value(v.ordinal)

fun value(v: Int) = Value(v)

fun rule(relation: Relation, id: Int): RuleSet = RuleSet(hashSetOf(Rule(relation, id)))

fun main() {
    val merger = Merger().apply {
        // ID, POSITION, COLOR, NATION, PET, DRINK, CIGARETTES
        add(Constraint(100, None, value(RED), value(ENGLISHMAN), None, None, None))
        add(Constraint(101, None, None, value(SPANIARD), value(DOG), None, None))
        add(Constraint(102, None, value(GREEN), None, None, value(COFFEE), None))
        add(Constraint(103, None, None, value(UKRAINIAN), None, value(TEA), None))
        add(Constraint(104, None, value(IVORY), None, None, None, None))
        add(Constraint(105, rule(imRight, 104), value(GREEN), None, None, None, None))
        add(Constraint(106, None, None, None, value(SNAILS), None, value(OLD_GOLD)))
        add(Constraint(107, None, value(YELLOW), None, None, None, value(KOOLS)))
        add(Constraint(108, value(2), None, None, None, value(MILK), None))
        add(Constraint(109, value(0), None, value(NORWEGIAN), None, None, None))
        add(Constraint(110, None, None, None, value(FOX), None, None))
        add(Constraint(111, rule(nextTo, 110), None, None, None, None, value(CHESTERFIELDS)))
        add(Constraint(112, None, None, None, value(HORSE), None, None))
        add(Constraint(113, rule(nextTo, 112), None, None, None, None, value(KOOLS)))
        add(Constraint(114, None, None, None, None, value(ORANGE_JUICE), value(LUCKY_STRIKE)))
        add(Constraint(115, None, None, value(JAPANESE), None, None, value(PARLIAMENTS)))
        add(Constraint(116, None, value(BLUE), None, None, None, None))
        add(Constraint(117, rule(nextTo, 116), None, value(NORWEGIAN), None, None, None))
        add(Constraint(118, None, None, None, value(ZEBRA), None, None))
        add(Constraint(119, None, None, None, None, value(WATER), None))
    }.merge()
    for (c in merger.constraints) {
        println(c.show())
    }
}
