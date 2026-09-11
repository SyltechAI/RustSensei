package com.sylvester.rustsensei.work

import com.sylvester.rustsensei.work.StudyReminderWorker.Companion.isQuietHour
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reminder worker repeats every 4 hours. Before the quiet-hours guard that
 * meant a "don't break your streak" notification at 01:00 and again at 05:00.
 */
class StudyReminderQuietHoursTest {

    @Test
    fun `the small hours are quiet`() {
        listOf(0, 1, 3, 5, 8).forEach {
            assertTrue("hour $it should be quiet", isQuietHour(it))
        }
    }

    @Test
    fun `late evening is quiet`() {
        listOf(21, 22, 23).forEach {
            assertTrue("hour $it should be quiet", isQuietHour(it))
        }
    }

    @Test
    fun `the waking day is not quiet`() {
        (9..20).forEach {
            assertFalse("hour $it should allow reminders", isQuietHour(it))
        }
    }

    @Test
    fun `the window boundaries are inclusive at the start and exclusive at the end`() {
        assertFalse("09:00 is the first allowed hour", isQuietHour(9))
        assertTrue("21:00 is the first quiet hour", isQuietHour(21))
    }
}
