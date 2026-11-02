package com.arverse.arbattery.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            ArBatterySmallWidget().updateAll(applicationContext)
            ArBatteryMediumWidget().updateAll(applicationContext)
            ArBatteryLargeWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object WidgetUpdateManager {

    private const val PERIODIC_WIDGET_WORK_NAME = "arbattery_widget_periodic_update"

    fun schedulePeriodicUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WIDGET_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                ArBatterySmallWidget().updateAll(context)
                ArBatteryMediumWidget().updateAll(context)
                ArBatteryLargeWidget().updateAll(context)
            } catch (_: Exception) {
            }
        }
    }
}
