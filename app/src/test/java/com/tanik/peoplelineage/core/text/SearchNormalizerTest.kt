package com.tanik.peoplelineage.core.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchNormalizerTest {

    @Test
    fun normalizeText_stripsExtraSpacesAndPunctuation() {
        val normalized = SearchNormalizer.normalizeText("  Md.  Abdul-Rahman  ")

        assertThat(normalized).isEqualTo("md abdul rahman")
    }

    @Test
    fun normalizePhone_keepsOnlyDigits() {
        val normalized = SearchNormalizer.normalizePhone("+91 98765-43210")

        assertThat(normalized).isEqualTo("919876543210")
    }
}
