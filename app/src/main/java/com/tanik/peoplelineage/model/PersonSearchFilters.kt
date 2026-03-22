package com.tanik.peoplelineage.model

data class PersonSearchFilters(
    val query: String = "",
    val gender: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = "",
    val favoritesOnly: Boolean = false,
)
