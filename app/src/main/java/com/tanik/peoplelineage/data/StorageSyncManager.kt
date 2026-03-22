package com.tanik.peoplelineage.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tanik.peoplelineage.model.StorageMode
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class StorageSyncManager(context: Context) {

    private val appContext = context.applicationContext
    private val repository = PeopleRepository.getInstance(appContext)
    private val preferences = AppPreferences(appContext)
    private val storageEventDao = AppDatabaseProvider.get(appContext).storageEventDao()
    private val contentResolver = appContext.contentResolver

    fun getStorageMode(): StorageMode? = preferences.getStorageMode()

    fun getCloudTreeUri(): Uri? = preferences.getCloudTreeUri()?.let(Uri::parse)

    fun getCloudDocumentUri(): Uri? = preferences.getCloudDocumentUri()?.let(Uri::parse)

    fun getDriveBackupUri(): Uri? = preferences.getDriveBackupUri()?.let(Uri::parse)

    fun isCloudConfigured(): Boolean = getCloudDocumentUri() != null || getCloudTreeUri() != null

    fun isUsingLegacyCloudTree(): Boolean = getCloudDocumentUri() == null && getCloudTreeUri() != null

    fun isDriveBackupConfigured(): Boolean = getDriveBackupUri() != null

    fun getDriveLastBackupAt(): Long = preferences.getDriveLastBackupAt()

    suspend fun configureCloudDocument(documentUri: Uri): SyncResult {
        return runStorageEvent(
            eventType = StorageEventType.CLOUD_LINK,
            storageMode = StorageMode.CLOUD,
            locationUri = documentUri.toString(),
        ) {
            takePersistablePermissions(documentUri)
            preferences.setCloudDocumentUri(documentUri.toString())
            preferences.setCloudTreeUri(null)

            val hasLocalData = repository.hasPeople()
            val cloudSnapshot = readTextIfAvailable(documentUri).orEmpty().trim()

            if (cloudSnapshot.isNotBlank()) {
                if (!hasLocalData) {
                    repository.importSnapshotJson(cloudSnapshot)
                    EventOutcome(
                        result = SyncResult("Cloud sync file linked and local data imported."),
                        checksum = sha256(cloudSnapshot),
                    )
                } else {
                    EventOutcome(
                        result = SyncResult("Cloud sync file linked. Use Sync Now or Pull From Cloud."),
                        checksum = sha256(cloudSnapshot),
                    )
                }
            } else {
                val snapshotJson = repository.exportSnapshotJson()
                writeTextToUri(documentUri, snapshotJson)
                EventOutcome(
                    result = if (hasLocalData) {
                        SyncResult("Cloud sync file linked and first sync completed.")
                    } else {
                        SyncResult("Cloud sync file linked and initialized.")
                    },
                    checksum = sha256(snapshotJson),
                )
            }
        }
    }

    suspend fun pushLocalToCloud(): SyncResult {
        val cloudUri = getConfiguredCloudUriForWrite() ?: throw IllegalStateException("Cloud sync file not configured.")
        return runStorageEvent(
            eventType = StorageEventType.CLOUD_PUSH,
            storageMode = StorageMode.CLOUD,
            locationUri = cloudUri.toString(),
        ) {
            val snapshotJson = repository.exportSnapshotJson()
            writeTextToUri(cloudUri, snapshotJson)
            EventOutcome(
                result = SyncResult("Cloud sync completed."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    suspend fun pushLocalToCloudIfConfigured() {
        if (preferences.getStorageMode() == StorageMode.CLOUD && isCloudConfigured()) {
            runCatching { pushLocalToCloud() }
        }
    }

    suspend fun pullFromCloud(): SyncResult {
        val cloudUri = getConfiguredCloudUriForRead() ?: throw FileNotFoundException("Cloud sync file not found.")
        return runStorageEvent(
            eventType = StorageEventType.CLOUD_PULL,
            storageMode = StorageMode.CLOUD,
            locationUri = cloudUri.toString(),
        ) {
            val snapshotJson = readRequiredText(cloudUri)
            repository.importSnapshotJson(snapshotJson)
            EventOutcome(
                result = SyncResult("Cloud data imported."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    suspend fun importFromCloudIfLocalEmpty(): SyncResult? {
        if (preferences.getStorageMode() != StorageMode.CLOUD || !isCloudConfigured() || repository.hasPeople()) {
            return null
        }
        return runCatching { pullFromCloud() }.getOrNull()
    }

    suspend fun configureDriveBackupDocument(documentUri: Uri): SyncResult {
        return runStorageEvent(
            eventType = StorageEventType.DRIVE_LINK,
            storageMode = StorageMode.DRIVE,
            locationUri = documentUri.toString(),
        ) {
            takePersistablePermissions(documentUri)
            preferences.setDriveBackupUri(documentUri.toString())
            val snapshotJson = repository.exportSnapshotJson()
            writeTextToUri(documentUri, snapshotJson)
            markDriveBackupCompleted()
            syncBackgroundTasks()
            EventOutcome(
                result = SyncResult("Drive backup file linked. Automatic backup runs every 12 hours."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    suspend fun backupToConfiguredDrive(): SyncResult {
        val driveUri = getDriveBackupUri() ?: throw IllegalStateException("Drive backup file not configured.")
        return runStorageEvent(
            eventType = StorageEventType.DRIVE_BACKUP,
            storageMode = StorageMode.DRIVE,
            locationUri = driveUri.toString(),
        ) {
            val snapshotJson = repository.exportSnapshotJson()
            writeTextToUri(driveUri, snapshotJson)
            markDriveBackupCompleted()
            EventOutcome(
                result = SyncResult("Drive backup saved successfully."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    suspend fun backupToDriveIfConfigured(): SyncResult? {
        if (preferences.getStorageMode() != StorageMode.DRIVE || !isDriveBackupConfigured()) {
            return null
        }
        return runCatching { backupToConfiguredDrive() }.getOrNull()
    }

    suspend fun backupToDocument(targetUri: Uri): SyncResult {
        return runStorageEvent(
            eventType = StorageEventType.DRIVE_BACKUP,
            storageMode = StorageMode.DRIVE,
            locationUri = targetUri.toString(),
        ) {
            takePersistablePermissions(targetUri)
            val snapshotJson = repository.exportSnapshotJson()
            writeTextToUri(targetUri, snapshotJson)
            EventOutcome(
                result = SyncResult("Backup saved successfully."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    suspend fun restoreFromDocument(sourceUri: Uri): SyncResult {
        return runStorageEvent(
            eventType = StorageEventType.DRIVE_RESTORE,
            storageMode = StorageMode.DRIVE,
            locationUri = sourceUri.toString(),
        ) {
            takePersistablePermissions(sourceUri)
            val snapshotJson = readRequiredText(sourceUri)
            repository.importSnapshotJson(snapshotJson)
            EventOutcome(
                result = SyncResult("Backup restored successfully."),
                checksum = sha256(snapshotJson),
            )
        }
    }

    fun syncBackgroundTasks() {
        if (preferences.getStorageMode() == StorageMode.DRIVE && isDriveBackupConfigured()) {
            val workRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(AUTO_BACKUP_INTERVAL_HOURS, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                UNIQUE_DRIVE_BACKUP_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )
        } else {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_DRIVE_BACKUP_WORK)
        }
    }

    private fun takePersistablePermissions(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            contentResolver.takePersistableUriPermission(uri, flags)
        }
    }

    private fun getConfiguredCloudUriForWrite(): Uri? {
        getCloudDocumentUri()?.let { return it }
        return getCloudTreeUri()?.let(::getOrCreateCloudFileUri)
    }

    private fun getConfiguredCloudUriForRead(): Uri? {
        getCloudDocumentUri()?.let { return it }
        return getCloudTreeUri()?.let(::findCloudFileUri)
    }

    private fun readTextIfAvailable(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
        } catch (_: FileNotFoundException) {
            null
        }
    }

    private fun readRequiredText(sourceUri: Uri): String {
        return contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: throw FileNotFoundException("Unable to open source file.")
    }

    private fun writeTextToUri(targetUri: Uri, snapshotJson: String) {
        contentResolver.openOutputStream(targetUri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(snapshotJson)
        } ?: throw FileNotFoundException("Unable to open target file.")
    }

    private fun markDriveBackupCompleted() {
        preferences.setDriveLastBackupAt(System.currentTimeMillis())
    }

    private suspend fun runStorageEvent(
        eventType: StorageEventType,
        storageMode: StorageMode,
        locationUri: String,
        block: suspend () -> EventOutcome,
    ): SyncResult {
        val startedAt = System.currentTimeMillis()
        return try {
            val outcome = block()
            storageEventDao.insertEvent(
                StorageEventEntity(
                    eventType = eventType.name,
                    status = StorageEventStatus.SUCCESS.name,
                    storageMode = storageMode.value,
                    locationUri = locationUri,
                    checksum = outcome.checksum,
                    message = outcome.result.message,
                    startedAt = startedAt,
                    completedAt = System.currentTimeMillis(),
                ),
            )
            outcome.result
        } catch (error: Throwable) {
            storageEventDao.insertEvent(
                StorageEventEntity(
                    eventType = eventType.name,
                    status = StorageEventStatus.FAILURE.name,
                    storageMode = storageMode.value,
                    locationUri = locationUri,
                    checksum = "",
                    message = error.message.orEmpty(),
                    startedAt = startedAt,
                    completedAt = System.currentTimeMillis(),
                ),
            )
            throw error
        }
    }

    private fun getOrCreateCloudFileUri(treeUri: Uri): Uri {
        return findCloudFileUri(treeUri)
            ?: createChildDocument(treeUri, MIME_TYPE_JSON, CLOUD_FILE_NAME)
            ?: throw FileNotFoundException("Unable to create cloud sync file.")
    }

    private fun findCloudFileUri(treeUri: Uri): Uri? {
        return findChildDocumentUri(treeUri, CLOUD_FILE_NAME)
    }

    private fun findChildDocumentUri(treeUri: Uri, displayName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        contentResolver.query(
            childrenUri,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == displayName) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idIndex))
                }
            }
        }
        return null
    }

    private fun createChildDocument(treeUri: Uri, mimeType: String, displayName: String): Uri? {
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        return DocumentsContract.createDocument(contentResolver, parentDocumentUri, mimeType, displayName)
    }

    private fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    data class SyncResult(
        val message: String,
    )

    private data class EventOutcome(
        val result: SyncResult,
        val checksum: String,
    )

    companion object {
        private const val CLOUD_FILE_NAME = "people-lineage-cloud.json"
        private const val MIME_TYPE_JSON = "application/json"
        private const val UNIQUE_DRIVE_BACKUP_WORK = "people_lineage_drive_backup"
        private const val AUTO_BACKUP_INTERVAL_HOURS = 12L
    }
}
