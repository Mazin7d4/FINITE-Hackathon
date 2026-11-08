package app.finite.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.Calendar
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito.*

class SessionPolicyTest {
    private lateinit var intentConstruction: MockedConstruction<Intent>

    @Before fun mockAndroidIntents() {
        intentConstruction = mockConstruction(Intent::class.java, withSettings().defaultAnswer(RETURNS_SELF))
    }

    @After fun closeAndroidIntentMock() {
        intentConstruction.close()
    }

    @Test fun missingConfigurationIsAlwaysActiveForBothApps() {
        val store = memoryStore()
        val now = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 3); set(Calendar.MINUTE, 0) }

        assertEquals(SessionStore.SUPPORTED_APPS, store.schedule().selectedApps)
        assertTrue(store.isWithinSchedule(now))
        store.start(2)
        assertEquals(1, store.increment("com.instagram.android", "legacy Reel").state.count)
        assertEquals(2, store.increment("YouTube Shorts", "legacy Short").state.count)
    }

    @Test fun emptyOrInvalidSavedAppsAndScheduleUseSafeDefaults() {
        val preferences = mock(SharedPreferences::class.java)
        `when`(preferences.getStringSet("selected_apps", null)).thenReturn(setOf("old-app-id"))
        `when`(preferences.contains("schedule_days")).thenReturn(true)
        `when`(preferences.contains("schedule_start")).thenReturn(true)
        `when`(preferences.contains("schedule_end")).thenReturn(true)
        `when`(preferences.getInt("schedule_days", SessionStore.ALL_DAYS_MASK)).thenReturn(0)
        `when`(preferences.getInt("schedule_start", 0)).thenReturn(2_000)
        `when`(preferences.getInt("schedule_end", 0)).thenReturn(-1)
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences("finite_session", Context.MODE_PRIVATE)).thenReturn(preferences)
        val store = SessionStore(context)

        assertEquals(SessionStore.SUPPORTED_APPS, store.schedule().selectedApps)
        assertEquals(SessionStore.ALL_DAYS_MASK, store.schedule().daysMask)
        assertEquals(0, store.schedule().startMinute)
        assertEquals(0, store.schedule().endMinute)
    }

    @Test fun invalidWeekdaysDoNotRetainAValidTimeWindow() {
        val store = memoryStore()

        store.configure(2, SessionStore.SUPPORTED_APPS, 0, 8 * 60, 22 * 60)

        assertEquals(SessionStore.ALL_DAYS_MASK, store.schedule().daysMask)
        assertEquals(0, store.schedule().startMinute)
        assertEquals(0, store.schedule().endMinute)
    }

    @Test fun invalidTimeDoesNotRetainValidWeekdays() {
        val store = memoryStore()
        val weekdays = (1 shl (Calendar.MONDAY - Calendar.SUNDAY)) or
            (1 shl (Calendar.FRIDAY - Calendar.SUNDAY))

        store.configure(2, SessionStore.SUPPORTED_APPS, weekdays, 8 * 60, 24 * 60)

        assertEquals(SessionStore.ALL_DAYS_MASK, store.schedule().daysMask)
        assertEquals(0, store.schedule().startMinute)
        assertEquals(0, store.schedule().endMinute)
    }

    @Test fun incompletePersistedScheduleUsesSafeDefaults() {
        val preferences = mock(SharedPreferences::class.java)
        `when`(preferences.contains("schedule_days")).thenReturn(true)
        `when`(preferences.contains("schedule_start")).thenReturn(false)
        `when`(preferences.contains("schedule_end")).thenReturn(false)
        `when`(preferences.getInt("schedule_days", 0)).thenReturn(1 shl (Calendar.MONDAY - Calendar.SUNDAY))
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences("finite_session", Context.MODE_PRIVATE)).thenReturn(preferences)

        val schedule = SessionStore(context).schedule()

        assertEquals(SessionStore.ALL_DAYS_MASK, schedule.daysMask)
        assertEquals(0, schedule.startMinute)
        assertEquals(0, schedule.endMinute)
    }

    @Test fun explicitWeekdaysRemainRestricted() {
        val store = memoryStore()
        store.configure(2, SessionStore.SUPPORTED_APPS,
            (1 shl (Calendar.MONDAY - Calendar.SUNDAY)) or (1 shl (Calendar.FRIDAY - Calendar.SUNDAY)),
            8 * 60, 22 * 60)
        val fridayNoon = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }
        val saturdayNoon = (fridayNoon.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        assertTrue(store.isWithinSchedule(fridayNoon))
        assertFalse(store.isWithinSchedule(saturdayNoon))
    }

    @Test fun overnightWindowUsesTheDayItStartedOn() {
        val preferences = mock(SharedPreferences::class.java)
        `when`(preferences.getInt("schedule_days", 0)).thenReturn(1 shl (Calendar.FRIDAY - Calendar.SUNDAY))
        `when`(preferences.contains("schedule_days")).thenReturn(true)
        `when`(preferences.contains("schedule_start")).thenReturn(true)
        `when`(preferences.contains("schedule_end")).thenReturn(true)
        `when`(preferences.getInt("schedule_start", 0)).thenReturn(22 * 60)
        `when`(preferences.getInt("schedule_end", 0)).thenReturn(2 * 60)
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

    private fun memoryStore(): SessionStore {
        val values = mutableMapOf<String, Any>()
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java, RETURNS_SELF)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(preferences.getStringSet(anyString(), nullable(Set::class.java as Class<MutableSet<String>>))).thenReturn(null)
        `when`(preferences.getInt(anyString(), anyInt())).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<Int>(1)
        }
        `when`(preferences.getBoolean(anyString(), anyBoolean())).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<Boolean>(1)
        }
        `when`(preferences.getString(anyString(), nullable(String::class.java))).thenAnswer {
            values[it.getArgument<String>(0)] as? String ?: it.getArgument<String?>(1)
        }
        `when`(preferences.contains(anyString())).thenAnswer { values.containsKey(it.getArgument<String>(0)) }
        `when`(editor.putInt(anyString(), anyInt())).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<Int>(1)
            editor
        }
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<Boolean>(1)
            editor
        }
        `when`(editor.putString(anyString(), nullable(String::class.java))).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<String?>(1).orEmpty()
            editor
        }
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences("finite_session", Context.MODE_PRIVATE)).thenReturn(preferences)
        return SessionStore(context)
    }
}
