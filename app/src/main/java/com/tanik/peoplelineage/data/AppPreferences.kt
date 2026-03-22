package com.tanik.peoplelineage.data

import android.content.Context
import com.tanik.peoplelineage.model.StorageMode

class AppPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStorageMode(): StorageMode? {
        return StorageMode.fromValue(preferences.getString(KEY_STORAGE_MODE, null))
    }

    fun setStorageMode(mode: StorageMode) {
        preferences.edit().putString(KEY_STORAGE_MODE, mode.value).apply()
    }

    fun getCloudTreeUri(): String? {
        return preferences.getString(KEY_CLOUD_TREE_URI, null)
    }

    fun setCloudTreeUri(uri: String?) {
        preferences.edit().putString(KEY_CLOUD_TREE_URI, uri).apply()
    }

    fun getCloudDocumentUri(): String? {
        return preferences.getString(KEY_CLOUD_DOCUMENT_URI, null)
    }

    fun setCloudDocumentUri(uri: String?) {
        preferences.edit().putString(KEY_CLOUD_DOCUMENT_URI, uri).apply()
    }

    fun getDriveBackupUri(): String? {
        return preferences.getString(KEY_DRIVE_BACKUP_URI, null)
    }

    fun setDriveBackupUri(uri: String?) {
        preferences.edit().putString(KEY_DRIVE_BACKUP_URI, uri).apply()
    }

    fun getDriveLastBackupAt(): Long {
        return preferences.getLong(KEY_DRIVE_LAST_BACKUP_AT, 0L)
    }

    fun setDriveLastBackupAt(timestamp: Long) {
        preferences.edit().putLong(KEY_DRIVE_LAST_BACKUP_AT, timestamp).apply()
    }

    companion object {
        private const val PREFS_NAME = "people_lineage_prefs"
        private const val KEY_STORAGE_MODE = "storage_mode"
        private const val KEY_CLOUD_TREE_URI = "cloud_tree_uri"
        private const val KEY_CLOUD_DOCUMENT_URI = "cloud_document_uri"
        private const val KEY_DRIVE_BACKUP_URI = "drive_backup_uri"
        private const val KEY_DRIVE_LAST_BACKUP_AT = "drive_last_backup_at"
    }
}
