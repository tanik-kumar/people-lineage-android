package com.tanik.peoplelineage.model

enum class StorageMode(val value: String) {
    LOCAL("local"),
    CLOUD("cloud"),
    DRIVE("drive"),
    ;

    companion object {
        fun fromValue(value: String?): StorageMode? {
            return entries.firstOrNull { it.value == value }
        }
    }
}
