package com.tanik.peoplelineage.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanik.peoplelineage.data.AppPreferences
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.model.PersonSearchFilters
import com.tanik.peoplelineage.model.StorageMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PeopleHomeViewModel @Inject constructor(
    private val repository: PeopleRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val selectedVillage = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val storageMode = MutableStateFlow(currentStorageModeLabel())

    val uiState = combine(
        selectedVillage,
        query,
        favoritesOnly,
        storageMode,
    ) { currentVillage, currentQuery, isFavoritesOnly, storageModeLabel ->
        HomeInputs(currentVillage, currentQuery, isFavoritesOnly, storageModeLabel)
    }.flatMapLatest { inputs ->
        repository.observePeople(
            PersonSearchFilters(
                query = if (inputs.selectedVillage != null) inputs.query else "",
                village = inputs.selectedVillage.orEmpty(),
                favoritesOnly = inputs.favoritesOnly,
            ),
        ).map { people ->
            val sections = people
                .map { person ->
                    val villageName = person.village.trim().ifBlank { "Unknown village" }
                    PersonListItemUiModel(
                        id = person.id,
                        title = person.fullName,
                        village = villageName,
                        subtitle = person.locationSummary(),
                        meta = person.metaSummary(),
                        isFavorite = person.isFavorite,
                    )
                }
                .groupBy { it.village }
                .toList()
                .sortedBy { it.first.lowercase() }
                .map { (villageName, groupedPeople) ->
                    VillageSectionUiModel(
                        villageName = villageName,
                        people = groupedPeople.sortedWith(
                            compareByDescending<PersonListItemUiModel> { it.isFavorite }
                                .thenBy { it.title.lowercase() },
                        ),
                    )
                }
            HomeUiState(
                isLoading = false,
                selectedVillage = inputs.selectedVillage,
                query = inputs.query,
                favoritesOnly = inputs.favoritesOnly,
                storageModeLabel = inputs.storageModeLabel,
                villages = sections,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(storageModeLabel = currentStorageModeLabel()),
    )

    fun onQueryChanged(value: String) {
        query.update { value }
    }

    fun selectVillage(villageName: String) {
        selectedVillage.value = villageName
        query.value = ""
    }

    fun clearVillageSelection() {
        selectedVillage.value = null
        query.value = ""
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.update { !it }
    }

    fun refreshStorageMode() {
        storageMode.value = currentStorageModeLabel()
    }

    private fun currentStorageModeLabel(): String {
        return when (preferences.getStorageMode()) {
            StorageMode.LOCAL -> "Local on this device"
            StorageMode.CLOUD -> "Cloud sync"
            StorageMode.DRIVE -> "Drive backup"
            null -> "Not selected"
        }
    }

    private fun com.tanik.peoplelineage.data.PersonEntity.locationSummary(): String {
        return listOf(district.trim(), state.trim())
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { "Location not available" }
    }

    private fun com.tanik.peoplelineage.data.PersonEntity.metaSummary(): String {
        val parts = buildList {
            phoneNumber.trim().takeIf { it.isNotBlank() }?.let(::add)
            gender.trim().takeIf { it.isNotBlank() }?.let(::add)
            age.trim().takeIf { it.isNotBlank() }?.let { add("Age $it") }
        }
        return parts.joinToString(" • ")
    }

    private data class HomeInputs(
        val selectedVillage: String?,
        val query: String,
        val favoritesOnly: Boolean,
        val storageModeLabel: String,
    )
}
