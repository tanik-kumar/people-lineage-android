package com.tanik.peoplelineage.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tanik.peoplelineage.feature.home.PeopleHomeScreen
import com.tanik.peoplelineage.feature.home.PeopleHomeViewModel
import com.tanik.peoplelineage.feature.person.PersonDetailScreen
import com.tanik.peoplelineage.feature.person.PersonDetailUiModel
import com.tanik.peoplelineage.feature.person.PersonDetailViewModel
import com.tanik.peoplelineage.feature.settings.SettingsScreen
import com.tanik.peoplelineage.feature.settings.SettingsViewModel
import com.tanik.peoplelineage.ui.HierarchyActivity
import com.tanik.peoplelineage.ui.PersonEditorActivity
import com.tanik.peoplelineage.ui.StorageSetupActivity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private object Destinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PERSON_DETAIL = "person/{personId}"

    fun personDetail(personId: Long) = "person/$personId"
}

@Composable
fun PeopleLineageNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
    ) {
        composable(Destinations.HOME) {
            val viewModel: PeopleHomeViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            RefreshOnResume { viewModel.refreshStorageMode() }
            PeopleHomeScreen(
                state = state,
                onQueryChanged = viewModel::onQueryChanged,
                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                onSelectVillage = viewModel::selectVillage,
                onBackToVillageList = viewModel::clearVillageSelection,
                onOpenPerson = { navController.navigate(Destinations.personDetail(it)) },
                onAddPerson = {
                    context.startActivity(PersonEditorActivity.createIntent(context))
                },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }

        composable(
            route = Destinations.PERSON_DETAIL,
            arguments = listOf(navArgument("personId") { type = NavType.LongType }),
        ) {
            val viewModel: PersonDetailViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            RefreshOnResume { viewModel.refresh() }
            PersonDetailScreen(
                state = state,
                onBack = navController::popBackStack,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenLinkedPerson = { navController.navigate(Destinations.personDetail(it)) },
                onEdit = { personId ->
                    context.startActivity(PersonEditorActivity.createIntent(context, personId = personId))
                },
                onOpenGraph = { personId ->
                    context.startActivity(HierarchyActivity.createIntent(context, personId))
                },
                onAddChild = { person ->
                    context.startActivity(
                        when (person.gender.trim().lowercase()) {
                            "male" -> PersonEditorActivity.createIntent(
                                context = context,
                                preselectFatherId = person.id,
                                prefillAddressFromId = person.id,
                            )
                            "female" -> PersonEditorActivity.createIntent(
                                context = context,
                                preselectMotherId = person.id,
                                prefillAddressFromId = person.id,
                            )
                            else -> PersonEditorActivity.createIntent(
                                context = context,
                                prefillAddressFromId = person.id,
                            )
                        },
                    )
                },
                onCall = { phone -> openDialer(context, phone) },
                onWhatsApp = { name, phone -> openWhatsApp(context, name, phone) },
                onShare = { person -> sharePersonDetails(context, person) },
            )
        }

        composable(Destinations.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            SettingsScreen(
                state = state,
                onBack = navController::popBackStack,
                onOpenLegacyStorageSettings = {
                    context.startActivity(StorageSetupActivity.createIntent(context, launchedFromSettings = true))
                },
            )
        }
    }
}

@Composable
private fun RefreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun openDialer(context: android.content.Context, rawPhoneNumber: String) {
    val dialable = toDialablePhoneNumber(rawPhoneNumber) ?: return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(dialable)}")))
}

private fun openWhatsApp(context: android.content.Context, personName: String, rawPhoneNumber: String) {
    val whatsappPhone = toWhatsAppPhoneNumber(rawPhoneNumber) ?: return
    val encodedMessage = URLEncoder.encode("Hello $personName", StandardCharsets.UTF_8.toString())
    val directIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("whatsapp://send?phone=$whatsappPhone&text=$encodedMessage"),
    ).apply {
        `package` = "com.whatsapp"
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://wa.me/$whatsappPhone?text=$encodedMessage"),
    )
    when {
        directIntent.resolveActivity(context.packageManager) != null -> context.startActivity(directIntent)
        webIntent.resolveActivity(context.packageManager) != null -> context.startActivity(webIntent)
    }
}

private fun sharePersonDetails(context: android.content.Context, person: PersonDetailUiModel) {
    val text = buildString {
        appendLine(person.fullName)
        appendLine(person.address)
        appendLine("Age: ${person.age}")
        appendLine("Phone: ${person.phone}")
        appendLine()
        appendLine("Father: ${person.father?.name ?: "Not linked yet"}")
        appendLine("Mother: ${person.mother?.name ?: "Not linked yet"}")
        appendLine("Spouses: ${person.spouses.joinToString { it.name }.ifBlank { "Not linked yet" }}")
        appendLine("Children: ${person.children.joinToString { it.name }.ifBlank { "Not linked yet" }}")
        appendLine()
        appendLine("Notes: ${person.notes}")
    }.trim()
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Person details: ${person.fullName}")
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share Details",
        ),
    )
}

private fun toDialablePhoneNumber(rawPhoneNumber: String): String? {
    val trimmed = rawPhoneNumber.trim()
    val digits = trimmed.filter(Char::isDigit)
    if (digits.length < 6) return null
    return if (trimmed.startsWith("+")) "+$digits" else digits
}

private fun toWhatsAppPhoneNumber(rawPhoneNumber: String): String? {
    val trimmed = rawPhoneNumber.trim()
    val digits = trimmed.filter(Char::isDigit)
    if (digits.length < 6) return null
    return when {
        trimmed.startsWith("+") -> digits
        digits.length == 10 -> "91$digits"
        else -> digits
    }
}
