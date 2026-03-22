package com.tanik.peoplelineage.model

data class SavePersonResult(
    val personId: Long,
    val fatherResult: RelationAddResult? = null,
    val motherResult: RelationAddResult? = null,
    val spouseResult: RelationAddResult? = null,
    val warnings: List<String> = emptyList(),
)
