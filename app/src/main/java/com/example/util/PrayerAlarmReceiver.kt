package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            PrayerAlarmScheduler.ACTION_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "Vakit"
                val targetMillis = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TARGET_MILLIS, 0L)
                val remainingMinutes = if (targetMillis > 0L) {
                    val diff = (targetMillis - System.currentTimeMillis() + 30000L) / 60000L
                    diff.coerceAtLeast(1L)
                } else {
                    10L
                }
                NotificationHelper.showPrayerApproachingNotification(context, prayerName, remainingMinutes)
                PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
            }
            PrayerAlarmScheduler.ACTION_DAILY_MOTIVATION -> {
                NotificationHelper.showMotivationNotification(context)
                PrayerAlarmScheduler.scheduleDailyMotivation(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
            }
        }
    }
}
