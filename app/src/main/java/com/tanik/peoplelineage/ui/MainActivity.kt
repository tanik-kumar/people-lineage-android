package com.tanik.peoplelineage.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.tanik.peoplelineage.data.AppPreferences
import com.tanik.peoplelineage.data.StorageSyncManager
import com.tanik.peoplelineage.navigation.PeopleLineageNavHost
import com.tanik.peoplelineage.ui.theme.PeopleLineageTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var syncManager: StorageSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = AppPreferences(this)
        val storageMode = preferences.getStorageMode()
        if (storageMode == null) {
            startActivity(StorageSetupActivity.createIntent(this))
            finish()
            return
        }

        syncManager = StorageSyncManager(this)
        syncManager.syncBackgroundTasks()

        setContent {
            PeopleLineageTheme {
                PeopleLineageNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            syncManager.syncBackgroundTasks()
            withContext(Dispatchers.IO) {
                syncManager.importFromCloudIfLocalEmpty()
            }
        }
    }
}
