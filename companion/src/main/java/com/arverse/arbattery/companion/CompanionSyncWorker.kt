package com.arverse.arbattery.companion

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.network.client.CompanionApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CompanionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val collector = BatteryCollector(appContext)
    private val preferences = CompanionPreferences(appContext)
    private val apiClient = CompanionApiClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val config = preferences.configFlow.first()
        if (!config.isPaired || config.authToken == null) {
            return@withContext Result.success()
        }

        try {
            val batteryInfo = collector.collectBatteryInfo()
            val reportDto = collector.toBatteryReportDto(config.deviceId, batteryInfo)

            val result = apiClient.reportBattery(
                host = config.hubIp,
                port = config.hubPort,
                token = config.authToken,
                report = reportDto
            )

            when (result) {
                is AppResult.Success<*> -> {
                    preferences.updateLastSync()
                    Result.success()
                }
                is AppResult.Error -> {
                    Result.retry()
                }
                is AppResult.Loading -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object CompanionSyncManager {

    private const val PERIODIC_SYNC_WORK_NAME = "arbattery_companion_periodic_sync"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CompanionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }
}
