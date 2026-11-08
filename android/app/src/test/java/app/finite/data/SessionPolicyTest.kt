package app.finite.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.*

class SessionPolicyTest {
    @Test fun overnightWindowUsesTheDayItStartedOn() {
        val preferences = mock(SharedPreferences::class.java)
        `when`(preferences.getInt("schedule_days", SessionStore.ALL_DAYS_MASK)).thenReturn(1 shl (Calendar.FRIDAY - Calendar.SUNDAY))
        `when`(preferences.getInt("schedule_start", 480)).thenReturn(22 * 60)
        `when`(preferences.getInt("schedule_end", 1320)).thenReturn(2 * 60)
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences("finite_session", Context.MODE_PRIVATE)).thenReturn(preferences)
        val store = SessionStore(context)

        val fridayLate = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 0) }
        val saturdayEarly = (fridayLate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 1) }
        val saturdayDay = (saturdayEarly.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 12) }

        assertTrue(store.isWithinSchedule(fridayLate))
        assertTrue(store.isWithinSchedule(saturdayEarly))
        assertFalse(store.isWithinSchedule(saturdayDay))
    }
}
