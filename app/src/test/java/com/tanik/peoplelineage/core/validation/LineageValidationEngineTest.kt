package com.tanik.peoplelineage.core.validation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LineageValidationEngineTest {

    @Test
    fun wouldCreateParentCycle_detectsDescendantLoop() {
        val childrenByParent = mapOf(
            1L to listOf(2L),
            2L to listOf(3L),
        )

        val result = LineageValidationEngine.wouldCreateParentCycle(
            parentId = 3L,
            childId = 1L,
            childrenByParent = childrenByParent,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun wouldCreateParentCycle_allowsUnrelatedEdge() {
        val childrenByParent = mapOf(
            1L to listOf(2L),
            2L to listOf(3L),
        )

        val result = LineageValidationEngine.wouldCreateParentCycle(
            parentId = 4L,
            childId = 1L,
            childrenByParent = childrenByParent,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun ageGapWarning_flagsVerySmallGap() {
        val warning = LineageValidationEngine.ageGapWarning(
            parentAge = "20",
            childAge = "15",
        )

        assertThat(warning).isEqualTo("Parent-child age gap looks unusually small.")
    }
}
