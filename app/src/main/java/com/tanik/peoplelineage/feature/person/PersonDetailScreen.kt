package com.tanik.peoplelineage.feature.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonDetailScreen(
    state: PersonDetailScreenState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLinkedPerson: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onOpenGraph: (Long) -> Unit,
    onAddChild: (PersonDetailUiModel) -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String, String) -> Unit,
    onShare: (PersonDetailUiModel) -> Unit,
) {
    val person = state.person
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.fullName ?: "Person detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite, enabled = person != null) {
                        Icon(
                            imageVector = if (person?.isFavorite == true) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorite",
                        )
                    }
                    IconButton(onClick = { person?.let(onShare) }, enabled = person != null) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (person == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            ) {
                Text(state.errorMessage ?: "Loading...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(person.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(person.address, style = MaterialTheme.typography.bodyLarge)
                        Text("Age: ${person.age}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Phone: ${person.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(person.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEdit(person.id) }) { Text("Edit") }
                    OutlinedButton(onClick = { onOpenGraph(person.id) }) { Text("Graph") }
                    OutlinedButton(onClick = { onAddChild(person) }) { Text("Add child") }
                    if (person.phone != "Not provided") {
                        OutlinedButton(onClick = { onCall(person.phone) }) { Text("Call") }
                        OutlinedButton(onClick = { onWhatsApp(person.fullName, person.phone) }) { Text("WhatsApp") }
                    }
                }
            }
            relationSection("Father", person.father, onOpenLinkedPerson)
            relationSection("Mother", person.mother, onOpenLinkedPerson)
            relationsSection("Spouses / Partners", person.spouses, onOpenLinkedPerson)
            relationsSection("Children", person.children, onOpenLinkedPerson)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.relationSection(
    title: String,
    person: LinkedPersonUiModel?,
    onOpenLinkedPerson: (Long) -> Unit,
) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    if (person == null) {
        item {
            Card {
                Text(
                    text = "$title not linked yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        item(key = "single-$title-${person.id}") {
            LinkedPersonCard(person = person, onOpenLinkedPerson = onOpenLinkedPerson)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.relationsSection(
    title: String,
    people: List<LinkedPersonUiModel>,
    onOpenLinkedPerson: (Long) -> Unit,
) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    if (people.isEmpty()) {
        item {
            Card {
                Text(
                    text = "No records linked yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        items(people, key = { it.id }) { person ->
            LinkedPersonCard(person = person, onOpenLinkedPerson = onOpenLinkedPerson)
        }
    }
}

@Composable
private fun LinkedPersonCard(
    person: LinkedPersonUiModel,
    onOpenLinkedPerson: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLinkedPerson(person.id) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(person.name, fontWeight = FontWeight.SemiBold)
            Text(person.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
