package com.tanik.peoplelineage.feature.home

data class PersonListItemUiModel(
    val id: Long,
    val title: String,
    val village: String,
    val subtitle: String,
    val meta: String,
    val isFavorite: Boolean,
)

data class VillageSectionUiModel(
    val villageName: String,
    val people: List<PersonListItemUiModel>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedVillage: String? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val storageModeLabel: String = "",
    val villages: List<VillageSectionUiModel> = emptyList(),
)
