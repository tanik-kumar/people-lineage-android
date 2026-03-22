package com.tanik.peoplelineage.feature.settings

data class StorageEventUiModel(
    val title: String,
    val status: String,
    val message: String,
    val checksum: String,
    val completedAtLabel: String,
)

data class SettingsUiState(
    val storageModeLabel: String = "",
    val recentEvents: List<StorageEventUiModel> = emptyList(),
)
