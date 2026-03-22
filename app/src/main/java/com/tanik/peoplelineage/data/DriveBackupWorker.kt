package com.tanik.peoplelineage.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tanik.peoplelineage.model.StorageMode

class DriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncManager = StorageSyncManager(applicationContext)
        if (syncManager.getStorageMode() != StorageMode.DRIVE || !syncManager.isDriveBackupConfigured()) {
            return Result.success()
        }
        return runCatching {
            syncManager.backupToConfiguredDrive()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
    }
}
