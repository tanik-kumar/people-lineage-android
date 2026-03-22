package com.tanik.peoplelineage.feature.person

data class LinkedPersonUiModel(
    val id: Long,
    val name: String,
    val location: String,
)

data class PersonDetailUiModel(
    val id: Long,
    val fullName: String,
    val gender: String,
    val address: String,
    val age: String,
    val phone: String,
    val notes: String,
    val isFavorite: Boolean,
    val father: LinkedPersonUiModel? = null,
    val mother: LinkedPersonUiModel? = null,
    val spouses: List<LinkedPersonUiModel> = emptyList(),
    val children: List<LinkedPersonUiModel> = emptyList(),
)

data class PersonDetailScreenState(
    val isLoading: Boolean = true,
    val person: PersonDetailUiModel? = null,
    val errorMessage: String? = null,
)
