import core.Problem

fun abc() {
    val problem = Problem<String, Int>()
    problem.addVariables(listOf("a", "b", "c"), (1..9).toList())
    var minvalue = 999.0 / (9 * 3)
    var minSolution: Map<String, Int> = emptyMap()
    for (solution in problem.getSolutionSequence()) {
        val a = solution.getValue("a")
        val b = solution.getValue("b")
        val c = solution.getValue("c")
        val value = (a * 100 + b * 10 + c).toDouble() / (a + b + c)
        if (value < minvalue) {
            minvalue = value
            minSolution = solution
        }
    }
    println("$minvalue : $minSolution")
}

fun main() {
    abc()
}