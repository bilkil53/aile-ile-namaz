package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.util.PrayerCalculator
import java.util.Calendar

object PrayerAlarmScheduler {

    const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
    const val ACTION_DAILY_MOTIVATION = "com.example.ACTION_DAILY_MOTIVATION"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_PRAYER_TARGET_MILLIS = "extra_prayer_target_millis"

    fun scheduleAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Saved coordinates or default Istanbul
        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

        val lat = prefs.getFloat("latitude", 41.0082f).toDouble()
        val lng = prefs.getFloat("longitude", 28.9784f).toDouble()

        val now = System.currentTimeMillis()

        // Schedule for today and tomorrow so alarms remain active without launching app
        for (dayOffset in 0..1) {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val times = PrayerCalculator.calculatePrayerTimes(lat, lng, calendar)

            val isFriday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val noonPrayerName = if (isFriday) "Cuma Namazı" else "Öğle"

            val todayDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
            val cachedDate = prefs.getString("cached_diyanet_date", null)
            val useCachedDiyanet = dayOffset == 0 && cachedDate == todayDateKey && !prefs.getString("cached_diyanet_ogle", null).isNullOrBlank()

            val prayerList = if (useCachedDiyanet) {
                listOf(
                    Pair("Sabah", prefs.getString("cached_diyanet_imsak", times.imsak) ?: times.imsak),
                    Pair(noonPrayerName, prefs.getString("cached_diyanet_ogle", times.ogle) ?: times.ogle),
                    Pair("İkindi", prefs.getString("cached_diyanet_ikindi", times.ikindi) ?: times.ikindi),
                    Pair("Akşam", prefs.getString("cached_diyanet_aksam", times.aksam) ?: times.aksam),
                    Pair("Yatsı", prefs.getString("cached_diyanet_yatsi", times.yatsi) ?: times.yatsi)
                )
            } else {
                listOf(
                    Pair("Sabah", times.imsak),
                    Pair(noonPrayerName, times.ogle),
                    Pair("İkindi", times.ikindi),
                    Pair("Akşam", times.aksam),
                    Pair("Yatsı", times.yatsi)
                )
            }

            prayerList.forEachIndexed { index, (name, timeStr) ->
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                    val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

                    val prayerTargetCal = (calendar.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val alarmCal = (prayerTargetCal.clone() as Calendar).apply {
                        // 10 dakika öncesi hatırlatma
                        add(Calendar.MINUTE, -10)
                    }

                    if (alarmCal.timeInMillis > now) {
                        val requestCode = 2000 + (dayOffset * 10) + index
                        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = ACTION_PRAYER_ALARM
                            putExtra(EXTRA_PRAYER_NAME, name)
                            putExtra(EXTRA_PRAYER_TARGET_MILLIS, prayerTargetCal.timeInMillis)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        setReliableAlarm(alarmManager, alarmCal.timeInMillis, pendingIntent, context)
                    }
                }
            }
        }

        scheduleDailyMotivation(context)
    }

    private fun cancelAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (dayOffset in 0..1) {
            for (index in 0..4) {
                val requestCode = 2000 + (dayOffset * 10) + index
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_ALARM
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    fun scheduleDailyMotivation(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Her gün saat 12:00'de: Ya dua, ya hadis, ya da özlü bir söz (1 kereye mahsus)
        val targetHour = 12
        val targetMinute = 0

        val now = System.currentTimeMillis()
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
        val prefs = context.getSharedPreferences("daily_notification_prefs", Context.MODE_PRIVATE)
        val alreadySentToday = prefs.getString("last_sent_motivation_date", null) == todayStr

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Eğer saat 12:00 geçtiyse veya bugün zaten 1 kere gönderildiyse yarın saat 12:00'ye kur
            if (timeInMillis <= now || alreadySentToday) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_MOTIVATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1990,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}

        setReliableAlarm(alarmManager, target.timeInMillis, pendingIntent, context)
    }

    private fun setReliableAlarm(
        alarmManager: AlarmManager,
        timeMillis: Long,
        pendingIntent: PendingIntent,
        context: Context
    ) {
        // 1. Try AlarmClockInfo (wakes up even from deep Doze mode, bypasses power restrictions)
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val showIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null
            val clockInfo = AlarmManager.AlarmClockInfo(timeMillis, showIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            return
        } catch (_: Exception) {
            // Fallback
        }

        // 2. Try setExactAndAllowWhileIdle
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) { }

        // 3. Ultimate fallback: setAndAllowWhileIdle wakes up device in Doze mode without exact alarm permission
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) { }
    }
}
