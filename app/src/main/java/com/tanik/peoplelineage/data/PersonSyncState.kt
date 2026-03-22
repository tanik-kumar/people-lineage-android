package com.tanik.peoplelineage.data

enum class PersonSyncState {
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    CONFLICT,
}
