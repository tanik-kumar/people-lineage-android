package com.tanik.peoplelineage.model

data class AddressSeed(
    val village: String = "",
    val policeStation: String = "",
    val postOffice: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = "",
) {
    fun hasVillage(): Boolean = village.isNotBlank()

    fun hasAddressContext(): Boolean {
        return state.isNotBlank() &&
            country.isNotBlank() &&
            listOf(policeStation, postOffice, district).any { it.isNotBlank() }
    }
}
