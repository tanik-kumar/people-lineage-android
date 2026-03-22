package com.tanik.peoplelineage.model

enum class RelationAddResult {
    ADDED,
    ALREADY_EXISTS,
    CYCLE_DETECTED,
    SAME_PERSON,
}
