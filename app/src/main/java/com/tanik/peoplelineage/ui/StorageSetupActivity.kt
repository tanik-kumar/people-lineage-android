package com.tanik.peoplelineage.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tanik.peoplelineage.R
import com.tanik.peoplelineage.data.AppPreferences
import com.tanik.peoplelineage.data.StorageSyncManager
import com.tanik.peoplelineage.databinding.ActivityStorageSetupBinding
import com.tanik.peoplelineage.model.StorageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class StorageSetupActivity : ComponentActivity() {

    private lateinit var binding: ActivityStorageSetupBinding
    private lateinit var preferences: AppPreferences
    private lateinit var syncManager: StorageSyncManager
    private var hadModeOnEntry: Boolean = false
    private var launchedFromSettings: Boolean = false
    private var continueAfterCloudPicker: Boolean = false
    private var continueAfterDrivePicker: Boolean = false

    private val createCloudFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val shouldContinue = continueAfterCloudPicker
        continueAfterCloudPicker = false

        if (uri == null) {
            if (shouldContinue) {
                goToMain()
            }
            return@registerForActivityResult
        }

        performStorageAction(
            onSuccess = {
                refreshUi()
                if (shouldContinue) {
                    goToMain()
                }
            },
        ) {
            syncManager.configureCloudDocument(uri)
        }
    }

    private val linkCloudFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        performStorageAction(onSuccess = { refreshUi() }) {
            syncManager.configureCloudDocument(uri)
        }
    }

    private val chooseDriveBackupFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val shouldContinue = continueAfterDrivePicker
        continueAfterDrivePicker = false

        if (uri == null) {
            if (shouldContinue) {
                goToMain()
            }
            return@registerForActivityResult
        }

        performStorageAction(
            onSuccess = {
                refreshUi()
                if (shouldContinue) {
                    goToMain()
                }
            },
        ) {
            syncManager.configureDriveBackupDocument(uri)
        }
    }

    private val createBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        performStorageAction {
            syncManager.backupToDocument(uri)
        }
    }

    private val restoreBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        performStorageAction {
            syncManager.restoreFromDocument(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        syncManager = StorageSyncManager(this)
        syncManager.syncBackgroundTasks()
        hadModeOnEntry = preferences.getStorageMode() != null
        launchedFromSettings = intent.getBooleanExtra(EXTRA_LAUNCHED_FROM_SETTINGS, false)

        if (launchedFromSettings) {
            binding.toolbar.title = getString(R.string.settings)
            binding.toolbar.subtitle = getString(R.string.storage_settings_subtitle)
            binding.setupNoteText.text = getString(R.string.storage_settings_note)
        }

        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            if (!hadModeOnEntry && preferences.getStorageMode() == null) {
                finishAffinity()
            } else {
                finish()
            }
        }

        binding.localModeButton.setOnClickListener {
            selectMode(StorageMode.LOCAL)
        }
        binding.cloudModeButton.setOnClickListener {
            selectCloudMode()
        }
        binding.driveModeButton.setOnClickListener {
            selectDriveMode()
        }
        binding.createCloudFileButton.setOnClickListener {
            createCloudFileLauncher.launch(getString(R.string.cloud_file_name))
        }
        binding.linkCloudFileButton.setOnClickListener {
            linkCloudFileLauncher.launch(arrayOf("application/json", "*/*"))
        }
        binding.syncCloudButton.setOnClickListener {
            performStorageAction(onSuccess = { refreshUi() }) {
                syncManager.pushLocalToCloud()
            }
        }
        binding.pullCloudButton.setOnClickListener {
            performStorageAction(onSuccess = { refreshUi() }) {
                syncManager.pullFromCloud()
            }
        }
        binding.chooseDriveBackupFileButton.setOnClickListener {
            chooseDriveBackupFileLauncher.launch(getString(R.string.backup_file_name))
        }
        binding.backupDriveButton.setOnClickListener {
            if (syncManager.isDriveBackupConfigured()) {
                performStorageAction(onSuccess = { refreshUi() }) {
                    syncManager.backupToConfiguredDrive()
                }
            } else {
                createBackupLauncher.launch(getString(R.string.backup_file_name))
            }
        }
        binding.restoreDriveButton.setOnClickListener {
            restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
        }

        refreshUi()
    }

    private fun selectMode(mode: StorageMode) {
        preferences.setStorageMode(mode)
        syncManager.syncBackgroundTasks()
        refreshUi()

        if (!launchedFromSettings && !hadModeOnEntry) {
            goToMain()
        } else {
            Snackbar.make(binding.root, getString(R.string.storage_mode_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun selectCloudMode() {
        preferences.setStorageMode(StorageMode.CLOUD)
        syncManager.syncBackgroundTasks()
        refreshUi()

        if (!launchedFromSettings && !hadModeOnEntry) {
            continueAfterCloudPicker = true
            createCloudFileLauncher.launch(getString(R.string.cloud_file_name))
        } else {
            Snackbar.make(binding.root, getString(R.string.storage_mode_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun selectDriveMode() {
        preferences.setStorageMode(StorageMode.DRIVE)
        syncManager.syncBackgroundTasks()
        refreshUi()

        if (!launchedFromSettings && !hadModeOnEntry) {
            continueAfterDrivePicker = true
            chooseDriveBackupFileLauncher.launch(getString(R.string.backup_file_name))
        } else {
            Snackbar.make(binding.root, getString(R.string.storage_mode_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun refreshUi() {
        val mode = preferences.getStorageMode()
        binding.currentModeText.text = when (mode) {
            null -> getString(R.string.storage_mode_not_selected)
            StorageMode.LOCAL -> getString(R.string.storage_mode_current_local)
            StorageMode.CLOUD -> getString(R.string.storage_mode_current_cloud)
            StorageMode.DRIVE -> getString(R.string.storage_mode_current_drive)
        }

        binding.localInfoText.isVisible = mode == StorageMode.LOCAL
        binding.cloudActionsCard.isVisible = mode == StorageMode.CLOUD
        binding.driveActionsCard.isVisible = mode == StorageMode.DRIVE

        val cloudLinked = syncManager.isCloudConfigured()
        binding.cloudStatusText.text = when {
            !cloudLinked -> getString(R.string.cloud_status_not_linked)
            syncManager.isUsingLegacyCloudTree() -> getString(R.string.cloud_status_legacy_linked)
            else -> getString(R.string.cloud_status_linked)
        }

        binding.createCloudFileButton.text = getString(
            if (cloudLinked) R.string.replace_cloud_file else R.string.create_cloud_file,
        )
        binding.syncCloudButton.isEnabled = cloudLinked
        binding.pullCloudButton.isEnabled = cloudLinked

        val driveConfigured = syncManager.isDriveBackupConfigured()
        val lastBackupAt = syncManager.getDriveLastBackupAt()
        binding.driveStatusText.text = when {
            !driveConfigured -> getString(R.string.drive_status_not_linked)
            lastBackupAt > 0L -> getString(R.string.drive_status_linked_with_time, formatTimestamp(lastBackupAt))
            else -> getString(R.string.drive_status_linked)
        }
        binding.chooseDriveBackupFileButton.text = getString(
            if (driveConfigured) R.string.replace_drive_backup_file else R.string.choose_drive_backup_file,
        )
        binding.backupDriveButton.isEnabled = driveConfigured
    }

    private fun performStorageAction(
        onSuccess: (() -> Unit)? = null,
        action: suspend () -> StorageSyncManager.SyncResult,
    ) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    action()
                }
            }.onSuccess { result ->
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                onSuccess?.invoke()
            }.onFailure {
                Snackbar.make(
                    binding.root,
                    it.message ?: getString(R.string.storage_operation_failed),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    }

    private fun goToMain() {
        if (hadModeOnEntry) {
            finish()
            return
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val EXTRA_LAUNCHED_FROM_SETTINGS = "extra_launched_from_settings"

        fun createIntent(context: Context, launchedFromSettings: Boolean = false): Intent {
            return Intent(context, StorageSetupActivity::class.java).apply {
                putExtra(EXTRA_LAUNCHED_FROM_SETTINGS, launchedFromSettings)
            }
        }
    }
}
