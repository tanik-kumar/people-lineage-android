package com.tanik.peoplelineage.core.text

object SearchNormalizer {
    fun normalizeText(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizePhone(value: String): String {
        return value.filter(Char::isDigit)
    }
}
