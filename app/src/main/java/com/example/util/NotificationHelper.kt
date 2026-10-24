package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.util.DailyContentProvider

object NotificationHelper {

    const val CHANNEL_MOTIVATION_ID = "namaz_daily_motivation_11"
    const val CHANNEL_PRAYER_ID = "namaz_prayer_reminders"
    const val CHANNEL_PARTNER_ID = "namaz_partner_alerts_v2"
    const val CHANNEL_UPDATE_ID = "namaz_app_updates_v1"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val motivationChannel = NotificationChannel(
                CHANNEL_MOTIVATION_ID,
                "Günün Sözü ve Motivasyon",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Manevi ayet, dua ve motivasyon bildirimleri"
            }

            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER_ID,
                "Namaz Vakitleri & Diyanet Günlük Duaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaz vakitlerine 10 dakika kala gelen Diyanet dua, ayet ve hadis bildirimleri"
                enableVibration(true)
            }

            val partnerChannel = NotificationChannel(
                CHANNEL_PARTNER_ID,
                "Aile Havuzu Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aile fertleriniz veya eşiniz ibadetini eda ettiğinde gelen anlık bildirimler"
                enableVibration(true)
            }

            val updateChannel = NotificationChannel(
                CHANNEL_UPDATE_ID,
                "Uygulama Güncellemeleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yeni APK sürümleri ve özellik güncellemeleri"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(prayerChannel, motivationChannel, partnerChannel, updateChannel)
            )
        }
    }

    fun showPrayerApproachingNotification(
        context: Context,
        prayerName: String,
        minutesRemaining: Long,
        customDay: Int? = null
    ) {
        val cal = java.util.Calendar.getInstance()
        val isFriday = cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        val isNoonPrayer = prayerName.contains("öğle", ignoreCase = true) || prayerName.contains("ogle", ignoreCase = true)
        val isCuma = prayerName.contains("cuma", ignoreCase = true) || (isFriday && isNoonPrayer)
        val displayName = if (isCuma) "Cuma Namazı" else prayerName
        val title = if (isCuma) "🕌 Hayırlı Cumalar! Cuma Namazına 10 Dakika Kaldı" else "🕌 $displayName Vaktine 10 Dakika Kaldı"
        val quote = DailyContentProvider.getPrayerApproachingQuote(if (isCuma) "cuma" else prayerName, customDay)
        val message = "${quote.title}: \"${quote.text}\""
        val reminder = if (isCuma) {
            "Hayırlı Cumalar! Abdestinizi tazeleyip Cuma namazı ve hutbeye yetişmek için camiye hazırlanmayı unutmayın. 🤲"
        } else {
            "Abdestinizi alıp vakit namazına hazırlanmayı unutmayın. 🤲"
        }
        val detailedText = "$message\n\n— ${quote.source}\n\n$reminder"

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                context,
                3300,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_PRAYER_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(3300, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showMotivationNotification(context: Context, customDay: Int? = null) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val prefs = context.getSharedPreferences("daily_notification_prefs", Context.MODE_PRIVATE)
        val lastSent = prefs.getString("last_sent_motivation_date", null)
        if (lastSent == todayStr && customDay == null) {
            // Bugün saat 12:00 bildirimi zaten 1 kere gönderildi. Ard arda tekrar gönderme.
            return
        }

        val quote = DailyContentProvider.getRandomRotatingNotificationQuote(customDay)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                context,
                1100,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_MOTIVATION_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🕌 ${quote.category}")
            .setContentText("${quote.title}: ${quote.text}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${quote.title}\n\n\"${quote.text}\"\n\n— ${quote.source}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(1100, builder.build())
            prefs.edit().putString("last_sent_motivation_date", todayStr).apply()
        } catch (_: SecurityException) {
        }
    }

    fun showPartnerAlertNotification(context: Context, partnerName: String, actionText: String) {
        val normName = FamilyRoleHelper.normalizeRoleTitle(partnerName)
        val address = FamilyRoleHelper.formatAddressForReis(normName)
        val title = if (partnerName.isNotBlank()) "👨‍👩‍👧‍👦 $address" else "👨‍👩‍👧‍👦 Aile İbadet Bildirimi"

        val formattedActionText = if (
            actionText.contains("katıldı") ||
            actionText.contains("ayrıldı") ||
            actionText.startsWith(address, ignoreCase = true) ||
            actionText.startsWith(normName, ignoreCase = true) ||
            actionText.startsWith(partnerName, ignoreCase = true)
        ) {
            actionText
        } else {
            "$address $actionText"
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                context,
                4000,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_PARTNER_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(formattedActionText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(formattedActionText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            val notificationId = (System.currentTimeMillis() % 100000).toInt() + 4000
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showAppUpdateNotification(context: Context, versionTag: String, releaseNotes: String) {
        val title = "📲 Yeni Güncelleme Yayında: $versionTag"
        val message = "Aile ile Namaz'ın yeni versiyonu hazır! Yüklemek ve yeniliklere ulaşmak için dokunun."

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                context,
                8820,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\n$releaseNotes"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(8820, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
