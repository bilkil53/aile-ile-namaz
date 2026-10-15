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
        val quote = DailyContentProvider.getPrayerApproachingQuote(prayerName, customDay)
        val title = "🕌 $prayerName Vaktine 10 Dakika Kaldı"
        val message = "${quote.title}: \"${quote.text}\""
        val detailedText = "$message\n\n— ${quote.source}\n\nAbdestinizi alıp vakit namazına hazırlanmayı unutmayın. 🤲"

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
        } catch (_: SecurityException) {
        }
    }

    fun showPartnerAlertNotification(context: Context, partnerName: String, actionText: String) {
        val title = if (partnerName.isNotBlank()) "👨‍👩‍👧‍👦 Aile Ferdi: $partnerName" else "👨‍👩‍👧‍👦 Aile İbadet Bildirimi"

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
            .setContentText(actionText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(actionText))
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
