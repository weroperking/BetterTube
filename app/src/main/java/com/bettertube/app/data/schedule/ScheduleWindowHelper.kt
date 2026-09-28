package com.bettertube.app.data.schedule

import com.bettertube.app.domain.model.ScheduleConfig
import java.util.Calendar

object ScheduleWindowHelper {

    fun isInsideWindow(config: ScheduleConfig, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!config.enabled) {
            return true
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (!config.daysOfWeek.contains(dayOfWeek)) {
            return false
        }

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = config.startHour * 60 + config.startMinute
        val endMinutes = config.endHour * 60 + config.endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight window (e.g. 22:00 to 06:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
