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
                NotificationHelper.showPrayerApproachingNotification(context, prayerName, 10L)
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
