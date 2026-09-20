package com.bettertube.app.data.schedule

import android.content.Context
import android.util.Log
import com.bettertube.app.domain.model.ScheduleConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ScheduleConfigStore"
        private const val KEY_SCHEDULE = "schedule"
    }

    private val settingsFile = File(context.filesDir, "settings.json")
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ScheduleConfig> = _config.asStateFlow()

    fun getConfig(): ScheduleConfig = _config.value

    fun saveConfig(newConfig: ScheduleConfig): Result<Unit> {
        return try {
            val root = if (settingsFile.exists()) {
                try { JSONObject(settingsFile.readText()) } catch (e: Exception) { JSONObject() }
            } else {
                JSONObject()
            }

            val scheduleObj = JSONObject().apply {
                put("enabled", newConfig.enabled)
                put("startHour", newConfig.startHour)
                put("startMinute", newConfig.startMinute)
                put("endHour", newConfig.endHour)
                put("endMinute", newConfig.endMinute)
                val daysArray = JSONArray()
                newConfig.daysOfWeek.forEach { daysArray.put(it) }
                put("daysOfWeek", daysArray)
            }

            root.put(KEY_SCHEDULE, scheduleObj)

            val tmpFile = File(context.filesDir, "settings.json.tmp")
            tmpFile.writeText(root.toString(2))
            tmpFile.renameTo(settingsFile)

            _config.value = newConfig
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save schedule config", e)
            Result.failure(e)
        }
    }

    private fun loadConfig(): ScheduleConfig {
        return try {
            if (!settingsFile.exists()) return ScheduleConfig()
            val root = JSONObject(settingsFile.readText())
            if (!root.has(KEY_SCHEDULE)) return ScheduleConfig()
            val obj = root.getJSONObject(KEY_SCHEDULE)
            val enabled = obj.optBoolean("enabled", false)
            val startHour = obj.optInt("startHour", 22)
            val startMinute = obj.optInt("startMinute", 0)
            val endHour = obj.optInt("endHour", 6)
            val endMinute = obj.optInt("endMinute", 0)
            val daysSet = mutableSetOf<Int>()
            val daysArray = obj.optJSONArray("daysOfWeek")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    daysSet.add(daysArray.getInt(i))
                }
            } else {
                daysSet.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
            }
            ScheduleConfig(
                enabled = enabled,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                daysOfWeek = daysSet
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load schedule config", e)
            ScheduleConfig()
        }
    }
}
