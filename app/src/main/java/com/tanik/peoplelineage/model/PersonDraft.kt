package com.tanik.peoplelineage.model

data class PersonDraft(
    val id: Long? = null,
    val fullName: String,
    val gender: String,
    val age: String,
    val phoneNumber: String,
    val village: String,
    val policeStation: String,
    val postOffice: String,
    val district: String,
    val state: String,
    val country: String,
    val notes: String,
    val fatherId: Long? = null,
    val motherId: Long? = null,
    val spouseId: Long? = null,
)
