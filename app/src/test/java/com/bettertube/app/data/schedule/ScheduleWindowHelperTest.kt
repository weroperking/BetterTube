package com.bettertube.app.data.schedule

import com.bettertube.app.domain.model.ScheduleConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleWindowHelperTest {

    @Test
    fun isInsideWindow_returnsTrue_whenDisabled() {
        val config = ScheduleConfig(
            enabled = false,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(ScheduleWindowHelper.isInsideWindow(config, cal))
    }

    @Test
    fun isInsideWindow_returnsTrue_whenWithinSameDayRange() {
        val config = ScheduleConfig(
            enabled = true,
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 30,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(ScheduleWindowHelper.isInsideWindow(config, cal))
    }

    @Test
    fun isInsideWindow_returnsTrue_whenWithinOvernightRange_andAfterStart() {
        val config = ScheduleConfig(
            enabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ScheduleWindowHelper.isInsideWindow(config, cal))
    }

    @Test
    fun isInsideWindow_returnsTrue_whenWithinOvernightRange_andBeforeEnd() {
        val config = ScheduleConfig(
            enabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(ScheduleWindowHelper.isInsideWindow(config, cal))
    }

    @Test
    fun isInsideWindow_returnsFalse_whenOutsideRange() {
        val config = ScheduleConfig(
            enabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScheduleWindowHelper.isInsideWindow(config, cal))
    }

    @Test
    fun isInsideWindow_respectsDaysOfWeekFilter() {
        val config = ScheduleConfig(
            enabled = true,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        )
        val weekdayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        val weekendCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScheduleWindowHelper.isInsideWindow(config, weekdayCal))
        assertTrue(ScheduleWindowHelper.isInsideWindow(config, weekendCal))
    }
}
