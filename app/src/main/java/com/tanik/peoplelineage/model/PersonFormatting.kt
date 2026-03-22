package com.tanik.peoplelineage.model

import com.tanik.peoplelineage.data.PersonEntity

fun PersonEntity.shortLocation(fallback: String): String {
    val segments = listOf(village, district, state)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return segments.joinToString(" • ").ifBlank { fallback }
}

fun PersonEntity.fullAddress(fallback: String): String {
    val segments = listOf(village, policeStation, postOffice, district, state, country)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return segments.joinToString(", ").ifBlank { fallback }
}

fun PersonEntity.toAddressSeed(): AddressSeed {
    return AddressSeed(
        village = village,
        policeStation = policeStation,
        postOffice = postOffice,
        district = district,
        state = state,
        country = country,
    )
}

fun PersonEntity.hasAnyPhone(): Boolean = phoneNumber.trim().isNotBlank()
