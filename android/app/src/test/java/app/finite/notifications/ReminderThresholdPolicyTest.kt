package app.finite.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderThresholdPolicyTest {
    @Test fun showsWhenTheMilestoneIsReached() {
        assertTrue(ReminderThresholdPolicy.shouldShow(4, 4, 8, null, "2026-11-07"))
    }

    @Test fun doesNotShowBeforeTheMilestone() {
        assertFalse(ReminderThresholdPolicy.shouldShow(4, 3, 8, null, "2026-11-07"))
    }

    @Test fun onlyShowsOncePerDay() {
        assertFalse(ReminderThresholdPolicy.shouldShow(4, 5, 8, "2026-11-07", "2026-11-07"))
        assertTrue(ReminderThresholdPolicy.shouldShow(4, 5, 8, "2026-11-07", "2026-11-08"))
    }

    @Test fun leavesTheHardLimitToTheLimitOverlay() {
        assertFalse(ReminderThresholdPolicy.shouldShow(8, 8, 8, null, "2026-11-07"))
        assertFalse(ReminderThresholdPolicy.shouldShow(4, 8, 8, null, "2026-11-07"))
    }
}
