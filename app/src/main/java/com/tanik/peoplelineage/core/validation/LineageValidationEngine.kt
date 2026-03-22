package com.tanik.peoplelineage.core.validation

object LineageValidationEngine {

    fun wouldCreateParentCycle(
        parentId: Long,
        childId: Long,
        childrenByParent: Map<Long, List<Long>>,
    ): Boolean {
        val stack = ArrayDeque<Long>()
        val visited = mutableSetOf<Long>()
        stack.add(childId)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (!visited.add(current)) {
                continue
            }

            val descendants = childrenByParent[current].orEmpty()
            if (descendants.contains(parentId)) {
                return true
            }
            descendants.forEach(stack::addLast)
        }

        return false
    }

    fun ageGapWarning(parentAge: String, childAge: String): String? {
        val parentYears = parentAge.trim().toIntOrNull() ?: return null
        val childYears = childAge.trim().toIntOrNull() ?: return null
        val gap = parentYears - childYears

        return when {
            gap < 12 -> "Parent-child age gap looks unusually small."
            gap > 90 -> "Parent-child age gap looks unusually large."
            else -> null
        }
    }
}
