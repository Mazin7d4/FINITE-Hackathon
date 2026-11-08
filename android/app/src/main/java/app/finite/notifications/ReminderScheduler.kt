package app.finite.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.finite.MainActivity
import java.util.Calendar

/** Independent local reminders. Calendar arithmetic preserves local wall-clock time across DST. */
class ReminderScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun schedule(id: String, atMillis: Long, videoCount: Int, repeat: String) {
        cancelAlarm(id)
        prefs.edit().putStringSet(KEY_IDS, ids() + id)
            .putLong(key(id, KEY_AT), atMillis).putInt(key(id, KEY_COUNT), videoCount)
            .putString(key(id, KEY_REPEAT), repeat).apply()
        arm(id, atMillis)
    }

    fun cancel(id: String) {
        cancelAlarm(id)
        prefs.edit().putStringSet(KEY_IDS, ids() - id)
            .remove(key(id, KEY_AT)).remove(key(id, KEY_COUNT)).remove(key(id, KEY_REPEAT)).apply()
    }

    fun reschedule() {
        ids().toList().forEach { id ->
            val next = futureOccurrence(id, System.currentTimeMillis())
            if (next == null) cancel(id)
            else { prefs.edit().putLong(key(id, KEY_AT), next).apply(); arm(id, next) }
        }
    }

    private fun futureOccurrence(id: String, now: Long): Long? {
        val at = prefs.getLong(key(id, KEY_AT), 0L)
        val repeat = prefs.getString(key(id, KEY_REPEAT), NEVER).orEmpty()
        if (repeat == NEVER) return at.takeIf { it > now }
        val calendar = Calendar.getInstance().apply { timeInMillis = at }
        while (calendar.timeInMillis <= now) {
            if (repeat == WEEKLY) calendar.add(Calendar.WEEK_OF_YEAR, 1)
            else calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun arm(id: String, atMillis: Long) {
        val alarm = appContext.getSystemService(AlarmManager::class.java)
        val trigger = atMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(id))
            else alarm.setExact(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(id))
        } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(id)) }
    }

    private fun cancelAlarm(id: String) = appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(id))
    private fun pendingIntent(id: String): PendingIntent = PendingIntent.getBroadcast(
        appContext, REQUEST_CODE_BASE + (id.hashCode() and 0x7fff),
        Intent(appContext, ReminderReceiver::class.java).setData(Uri.parse("finite://reminder/$id")).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    private fun ids(): Set<String> = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
    private fun key(id: String, suffix: String) = "reminder_${id}_$suffix"

    companion object {
        const val DAILY = "Every day"
        const val WEEKLY = "Every week"
        const val NEVER = "Never"
        const val EXTRA_ID = "finite_reminder_id"
        private const val PREFERENCES = "finite_reminder"
        private const val KEY_IDS = "ids"
        private const val KEY_AT = "at"
        private const val KEY_COUNT = "count"
        private const val KEY_REPEAT = "repeat"
        private const val REQUEST_CODE_BASE = 739
        private const val CHANNEL_ID = "finite_reminders"
        fun show(context: Context, id: String, count: Int) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "FINITE reminders", NotificationManager.IMPORTANCE_DEFAULT))
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val intent = PendingIntent.getActivity(context, 740, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationManagerCompat.from(context).notify(740 + (id.hashCode() and 0x7fff), NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(app.finite.R.drawable.ic_finite).setContentTitle("FINITE reminder")
                .setContentText("You saved $count short videos for later.").setContentIntent(intent).setAutoCancel(true).build())
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val prefs = context.getSharedPreferences("finite_reminder", Context.MODE_PRIVATE)
        ReminderScheduler.show(context, id, prefs.getInt("reminder_${id}_count", 1))
        ReminderScheduler(context).reschedule()
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) ReminderScheduler(context).reschedule()
    }
}
