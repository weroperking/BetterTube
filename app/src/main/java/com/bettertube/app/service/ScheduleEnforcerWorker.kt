package com.bettertube.app.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bettertube.app.data.schedule.ScheduleConfigStore
import com.bettertube.app.data.schedule.ScheduleWindowHelper
import com.bettertube.app.domain.repository.Aria2Repository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduleEnforcerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aria2Repository: Aria2Repository,
    private val scheduleConfigStore: ScheduleConfigStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val config = scheduleConfigStore.getConfig()
            if (!config.enabled) {
                return Result.success()
            }

            val isInside = ScheduleWindowHelper.isInsideWindow(config)
            if (isInside) {
                aria2Repository.resumeAllPaused()
            } else {
                aria2Repository.pauseAllActive()
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("ScheduleEnforcerWorker", "Edge Case 4: Fail silently if aria2 daemon is stopped", e)
            Result.success()
        }
    }
}
