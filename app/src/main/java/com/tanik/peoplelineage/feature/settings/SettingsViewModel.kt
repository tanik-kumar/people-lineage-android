package com.tanik.peoplelineage.feature.settings

import androidx.lifecycle.ViewModel
import com.tanik.peoplelineage.data.AppPreferences
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.model.StorageMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: PeopleRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    val uiState = repository.observeRecentStorageEvents()
        .map { events ->
            SettingsUiState(
                storageModeLabel = when (preferences.getStorageMode()) {
                    StorageMode.LOCAL -> "Local on this device"
                    StorageMode.CLOUD -> "Cloud sync"
                    StorageMode.DRIVE -> "Drive backup"
                    null -> "Not selected"
                },
                recentEvents = events.map { event ->
                    StorageEventUiModel(
                        title = event.eventType.replace('_', ' '),
                        status = event.status,
                        message = event.message,
                        checksum = event.checksum.take(12),
                        completedAtLabel = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT,
                        ).format(Date(event.completedAt)),
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(
                storageModeLabel = when (preferences.getStorageMode()) {
                    StorageMode.LOCAL -> "Local on this device"
                    StorageMode.CLOUD -> "Cloud sync"
                    StorageMode.DRIVE -> "Drive backup"
                    null -> "Not selected"
                },
            ),
        )
}
