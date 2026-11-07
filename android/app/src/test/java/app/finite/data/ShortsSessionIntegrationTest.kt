package app.finite.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import app.finite.accessibility.ShortsCounter
import app.finite.accessibility.ShortsObservation
import app.finite.accessibility.ShortsScroll
import app.finite.accessibility.ShortsViewer
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.*

/** Real counter/store with mocked Android preferences and broadcast construction. */
class ShortsSessionIntegrationTest {
    @Test fun platformsShareCounterLimitAcknowledgementAndInactiveGuard() {
        mockConstruction(Intent::class.java, withSettings().defaultAnswer(RETURNS_SELF)).use {
            val context = memoryContext()
            val instagram = SessionStore(context)
            val youtube = SessionStore(context)
            val counter = ShortsCounter()
            instagram.start(3)
            assertFalse(instagram.increment("Instagram Reel").shouldShowLimit)
            val first = counter.update(ShortsObservation(ShortsViewer.CONFIRMED), 0).incrementReason!!
            assertFalse(youtube.increment(first).shouldShowLimit)
            counter.update(ShortsObservation(ShortsViewer.CONFIRMED, scroll =
                ShortsScroll(1_000, "reel_player_underlay", -550, -550, relativeOffset = true)), 1_000)
            val next = counter.update(ShortsObservation(ShortsViewer.CONFIRMED), 1_200).incrementReason!!
            assertTrue(youtube.increment(next).shouldShowLimit)
            assertEquals(3, instagram.snapshot().count)
            youtube.acknowledgeLimit()
            assertFalse(instagram.increment("Instagram Reel").shouldShowLimit)
            youtube.end()
            assertFalse(youtube.increment("late Short").shouldShowLimit)
            assertEquals(4, youtube.snapshot().count)
        }
    }

    @Test fun freshSessionAndExplicitCounterResetClearState() {
        mockConstruction(Intent::class.java, withSettings().defaultAnswer(RETURNS_SELF)).use {
            val store = SessionStore(memoryContext())
            val counter = ShortsCounter()
            store.start(1)
            store.increment(counter.update(ShortsObservation(ShortsViewer.CONFIRMED), 0).incrementReason!!)
            store.acknowledgeLimit()
            counter.reset() // Service reset receiver executes this before bridge startup.
            store.start(2)
            assertEquals(0, store.snapshot().count)
            assertFalse(store.snapshot().limitAcknowledged)
            assertFalse(store.increment(counter.update(ShortsObservation(ShortsViewer.CONFIRMED), 1)
                .incrementReason!!).shouldShowLimit)
            assertEquals(1, store.snapshot().count)
        }
    }

    private fun memoryContext(): Context {
        val values = mutableMapOf<String, Any>()
        val context = mock(Context::class.java)
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java, RETURNS_SELF)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences("finite_session", Context.MODE_PRIVATE)).thenReturn(preferences)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(preferences.getInt(anyString(), anyInt())).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<Int>(1)
        }
        `when`(preferences.getBoolean(anyString(), anyBoolean())).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<Boolean>(1)
        }
        `when`(preferences.getString(anyString(), nullable(String::class.java))).thenAnswer {
            values[it.getArgument<String>(0)] ?: it.getArgument<String?>(1)
        }
        `when`(editor.putInt(anyString(), anyInt())).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<Int>(1); editor
        }
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<Boolean>(1); editor
        }
        `when`(editor.putString(anyString(), anyString())).thenAnswer {
            values[it.getArgument<String>(0)] = it.getArgument<String>(1); editor
        }
        return context
    }
}
