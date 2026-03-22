package com.tanik.peoplelineage.feature.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.model.PersonDetailSnapshot
import com.tanik.peoplelineage.model.fullAddress
import com.tanik.peoplelineage.model.shortLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val repository: PeopleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val personId: Long = checkNotNull(savedStateHandle["personId"])
    private val _uiState = MutableStateFlow(PersonDetailScreenState())
    val uiState: StateFlow<PersonDetailScreenState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = PersonDetailScreenState(isLoading = true)
            val snapshot = repository.getPersonDetail(personId)
            if (snapshot == null) {
                _uiState.value = PersonDetailScreenState(
                    isLoading = false,
                    errorMessage = "Person not found.",
                )
                return@launch
            }
            repository.markPersonViewed(personId)
            _uiState.value = PersonDetailScreenState(
                isLoading = false,
                person = snapshot.toUiModel(),
            )
        }
    }

    fun toggleFavorite() {
        val currentPerson = _uiState.value.person ?: return
        viewModelScope.launch {
            repository.setFavorite(currentPerson.id, !currentPerson.isFavorite)
            refresh()
        }
    }

    private fun PersonDetailSnapshot.toUiModel(): PersonDetailUiModel {
        return PersonDetailUiModel(
            id = person.id,
            fullName = person.fullName,
            gender = person.gender,
            address = person.fullAddress("Location not available"),
            age = person.age.ifBlank { "Not provided" },
            phone = person.phoneNumber.ifBlank { "Not provided" },
            notes = person.notes.ifBlank { "No notes added." },
            isFavorite = person.isFavorite,
            father = father?.toLinkedPerson(),
            mother = mother?.toLinkedPerson(),
            spouses = spouses.map { it.toLinkedPerson() },
            children = children.map { it.toLinkedPerson() },
        )
    }

    private fun com.tanik.peoplelineage.data.PersonEntity.toLinkedPerson(): LinkedPersonUiModel {
        return LinkedPersonUiModel(
            id = id,
            name = fullName,
            location = shortLocation("Location not available"),
        )
    }
}
