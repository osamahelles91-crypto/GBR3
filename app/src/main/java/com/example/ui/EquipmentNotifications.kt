package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Locale

object EquipmentNotifications {

    const val CHANNEL_ID = "equipment_water_fill_channel"
    private const val CHANNEL_NAME = "تعبئة المياه والمعدات"

    const val ALERT_CHANNEL_ID = "equipment_urgent_alerts_channel_v3"
    private const val ALERT_CHANNEL_NAME = "التنبيهات العاجلة للمعدات"

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            // 1. Water filling progress channel (low importance, silent)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "متابعة تقدم تعبئة المياه التلقائية وشريط الإشعارات الحي"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // 2. Urgent alerts channel (high importance, sound and vibration enabled)
            if (notificationManager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
                val alertChannel = NotificationChannel(
                    ALERT_CHANNEL_ID,
                    ALERT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات عاجلة عند انقطاع مصدر المياه أو عدم استجابة المحرك"
                    
                    // Explicitly enable and configure sound
                    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                    setSound(defaultSoundUri, audioAttributes)
                    
                    // Explicitly enable and configure vibration
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    
                    // Enable lights
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                }
                notificationManager.createNotificationChannel(alertChannel)
            }
        }
    }

    fun updateEquipmentFillNotification(
        context: Context,
        lineIndex: Int,
        lineName: String,
        fillActive: Boolean,
        currentWeight: Double,
        targetWeight: Double
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(context)

        val notificationId = 4100 + lineIndex

        if (fillActive && targetWeight > 0) {
            val pct = ((currentWeight / targetWeight) * 100.0).coerceIn(0.0, 100.0)
            val progressMax = 1000
            val progressCurrent = (pct * 10.0).toInt().coerceIn(0, 1000)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formatCurr = String.format(Locale.US, "%.1f", currentWeight)
            val formatTarget = String.format(Locale.US, "%.1f", targetWeight)
            val formatPct = String.format(Locale.US, "%.1f", pct)
            val formatRemaining = String.format(Locale.US, "%.1f", maxOf(0.0, targetWeight - currentWeight))

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("🚰 جاري تعبئة المياه ($lineName)")
                .setContentText("الحالي: $formatCurr كجم / المستهدف: $formatTarget كجم ($formatPct%) - متبقي $formatRemaining كجم")
                .setSubText("$formatPct%")
                .setProgress(progressMax, progressCurrent, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(notificationId, builder.build())
        } else {
            notificationManager.cancel(notificationId)
        }
    }

    fun showEquipmentFillCompletedNotification(
        context: Context,
        lineIndex: Int,
        lineName: String,
        finalWeight: Double,
        targetWeight: Double
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(context)

        val notificationId = 4100 + lineIndex
        val formatFinal = String.format(Locale.US, "%.1f", finalWeight)
        val formatTarget = String.format(Locale.US, "%.1f", targetWeight)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✅ اكتملت تعبئة المياه ($lineName)")
            .setContentText("الوزن النهائي المحقق: $formatFinal كجم من المستهدف: $formatTarget كجم")
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(notificationId, builder.build())
    }

    fun showEquipmentFillFailedNotification(
        context: Context,
        lineIndex: Int,
        lineName: String,
        currentWeight: Double,
        targetWeight: Double,
        reason: String = "انقطاع مصدر المياه أو عدم زيادة الوزن بعد فتح الصمام"
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(context)

        // Cancel the progress notification if running
        notificationManager.cancel(4100 + lineIndex)

        val notificationId = 4200 + lineIndex
        val formatCurr = String.format(Locale.US, "%.1f", currentWeight)
        val formatTarget = String.format(Locale.US, "%.1f", targetWeight)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("goToEquipment", true)
            putExtra("lineIndex", lineIndex)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ توقفت تعبئة المياه ($lineName) - لم يكتمل الهدف")
            .setContentText("توقف الضخ عند $formatCurr كجم من أصل $formatTarget كجم ($reason)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "توقفت عملية التعبئة تلقائياً قبل بلوغ الوزن المستهدف.\n" +
                    "• الوزن الفعلي المحقق: $formatCurr كجم\n" +
                    "• الوزن المستهدف: $formatTarget كجم\n" +
                    "• السبب المرجح: $reason"
                )
            )
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelEquipmentFillNotification(context: Context, lineIndex: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(4100 + lineIndex)
    }

    fun showControllerHardwareAlertNotification(
        context: Context,
        alert: ControllerHardwareAlert
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(context)

        val notificationId = 5100 + alert.id + (alert.lineIndex * 1000)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("goToAlerts", true)
            putExtra("alertLineIndex", alert.lineIndex)
            putExtra("alertId", alert.id)
            putExtra("alertType", alert.type)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = when (alert.type) {
            "NO_SENSOR_RESPONSE" -> "⚠️"
            "WATER_SUPPLY_FAILURE" -> "🚰❌"
            "SCALE_DISCONNECTED_WHILE_FILLING" -> "⚖️❌"
            else -> "🚨"
        }

        val typeText = when (alert.type) {
            "NO_SENSOR_RESPONSE" -> "عدم استجابة المستشعر"
            "WATER_SUPPLY_FAILURE" -> "انقطاع مصدر المياه"
            "SCALE_DISCONNECTED_WHILE_FILLING" -> "انقطاع اتصال الميزان"
            else -> alert.type
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("$emoji تنبيه عتادي [$typeText] - ${alert.lineName}")
            .setContentText(alert.message)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(notificationId, builder.build())
    }
}
