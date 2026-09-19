package com.quremed.medtime.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quremed.medtime.cloud.CloudClient
import com.quremed.medtime.data.AppStore

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = AppStore(applicationContext)
        val share = store.cloudOwnerShare() ?: return Result.success()
        val profile = store.profile() ?: return Result.success()

        return CloudClient.updateOwnerShare(
            share = share,
            profile = profile,
            medicines = store.medications(),
            logs = store.logs()
        ).fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 4) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        private const val UNIQUE_WORK = "medtime-cloud-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
