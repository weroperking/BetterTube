package com.bettertube.app.data.schedule

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bettertube.app.domain.model.ScheduleConfig
import com.bettertube.app.service.ScheduleEnforcerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: ScheduleConfigStore
) {
    companion object {
        const val UNIQUE_WORK_NAME = "schedule_enforcer"
    }

    fun apply(config: ScheduleConfig) {
        val workManager = WorkManager.getInstance(context)
        if (!config.enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        // Calculate initial delay to the next 15-minute boundary
        val now = Calendar.getInstance()
        val minute = now.get(Calendar.MINUTE)
        val minutesToNextBoundary = 15 - (minute % 15)
        val initialDelayMinutes = if (minutesToNextBoundary == 0) 15L else minutesToNextBoundary.toLong()

        val periodicRequest = PeriodicWorkRequestBuilder<ScheduleEnforcerWorker>(
            15, TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
