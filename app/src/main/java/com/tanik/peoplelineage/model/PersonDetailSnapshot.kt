package com.tanik.peoplelineage.model

import com.tanik.peoplelineage.data.PersonEntity

data class PersonDetailSnapshot(
    val person: PersonEntity,
    val father: PersonEntity?,
    val mother: PersonEntity?,
    val spouses: List<PersonEntity>,
    val children: List<PersonEntity>,
)
