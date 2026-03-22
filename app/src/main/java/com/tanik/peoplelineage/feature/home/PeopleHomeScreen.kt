package com.tanik.peoplelineage.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleHomeScreen(
    state: HomeUiState,
    onQueryChanged: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSelectVillage: (String) -> Unit,
    onBackToVillageList: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val selectedVillage = state.selectedVillage
    val totalPeople = state.villages.sumOf { it.people.size }
    val visiblePeople = state.villages.flatMap { it.people }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedVillage ?: "People Lineage",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = state.storageModeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (selectedVillage != null) {
                        IconButton(onClick = onBackToVillageList) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to villages",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerson) {
                Icon(Icons.Rounded.Add, contentDescription = "Add person")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (selectedVillage == null) "Villages" else "People in $selectedVillage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (selectedVillage == null) {
                                "$totalPeople people across ${state.villages.size} villages"
                            } else {
                                "${visiblePeople.size} people in the selected village"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (selectedVillage == null) {
                                "Tap any village to open its people list"
                            } else {
                                "Search and open any person from this village"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onToggleFavoritesOnly,
                        label = {
                            Text(if (state.favoritesOnly) "Showing favorites" else "All people")
                        },
                    )
                }
            }
            if (selectedVillage != null) {
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.query,
                        onValueChange = onQueryChanged,
                        label = { Text("Search by name, phone, district, state") },
                        singleLine = true,
                    )
                }
            }
            if (state.isLoading) {
                item {
                    Card {
                        Text(
                            text = "Loading family records...",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (selectedVillage == null) {
                if (state.villages.isEmpty() && !state.isLoading) {
                    item {
                        Card {
                            Text(
                                text = "No villages found. Add the first record to create a village view.",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                items(state.villages, key = { it.villageName }) { village ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVillage(village.villageName) },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = village.villageName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = "${village.people.size} people",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            } else {
                if (visiblePeople.isEmpty() && !state.isLoading) {
                    item {
                        Card {
                            Text(
                                text = "No people found in this village for the current filter.",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                items(visiblePeople, key = { it.id }) { person ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPerson(person.id) },
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = person.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = person.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (person.meta.isNotBlank()) {
                                Text(
                                    text = person.meta,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
