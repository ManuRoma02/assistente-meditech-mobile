package com.example.assistentemeditechmobile.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.assistentemeditechmobile.MainActivity
import com.example.assistentemeditechmobile.R
import com.example.assistentemeditechmobile.domain.EmailItem

object EmailNotificationManager {

    const val CHANNEL_ID = "meditech_email_recap_channel"
    const val CHANNEL_NAME = "Email Cliniche & Recap AI"

    const val ACTION_OPEN_EMAIL = "com.example.assistentemeditechmobile.ACTION_OPEN_EMAIL"
    const val ACTION_MARK_READ = "com.example.assistentemeditechmobile.ACTION_MARK_READ"
    const val ACTION_ACCEPT_MEETING = "com.example.assistentemeditechmobile.ACTION_ACCEPT_MEETING"

    const val EXTRA_EMAIL_ID = "extra_email_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_MEETING_SLOT = "extra_meeting_slot"
    const val EXTRA_MEETING_TITLE = "extra_meeting_title"
    const val EXTRA_MEETING_DURATION = "extra_meeting_duration"
    const val EXTRA_MEETING_CONTACT = "extra_meeting_contact"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per nuove comunicazioni sanitarie con riassunto IA immediato"
                enableLights(true)
                lightColor = 0xFF0A7EA4.toInt() // Meditech Cyan
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 90, 180)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showEmailNotification(context: Context, email: EmailItem) {
        // Verifica permessi Android 13+ (POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(context)

        val notificationId = email.id.hashCode()

        // Intent per aprire l'app direttamente sull'email
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_EMAIL
            putExtra(EXTRA_EMAIL_ID, email.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent azione: Segna come Letta (Background Broadcast)
        val markReadIntent = Intent(context, EmailActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_EMAIL_ID, email.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Large icon (Avatar MeDiTech)
        val largeIconBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.meditech_avatar)
        }.getOrNull()

        // Contenuto esteso del recap AI con formattazione pulita
        val cleanSender = email.sender.substringBefore('<').trim()
        val bigText = buildString {
            appendLine("💡 **Sintesi AI MeDiTech**:")
            appendLine(email.summary.ifBlank { email.bodySnippet })
            if (email.hasAppointmentProposal && email.suggestedSlot.isNotBlank()) {
                appendLine()
                appendLine("📅 **Proposta Incontro**: ${email.suggestedSlot}")
            }
            if (email.attachments.isNotEmpty()) {
                appendLine()
                appendLine("📎 **Allegati**: ${email.attachments.joinToString(", ")}")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_email_recap)
            .setContentTitle("📬 $cleanSender")
            .setContentText(email.summary.ifBlank { email.subject })
            .setSubText(if (email.isVip) "⭐ VIP MeDiTech • Recap AI" else "MeDiTech AI • 100% Locale")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📬 $cleanSender • ${email.subject}")
                    .bigText(bigText)
                    .setSummaryText("Sintesi Clinica On-Device (Zero Cloud)")
            )
            .setColor(0xFF009688.toInt()) // Meditech Cyan/Teal
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setGroup("meditech_email_recap_group")
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_stat_email_recap,
                "Leggi Recap 📖",
                openPendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Segna Letta ✓",
                markReadPendingIntent
            )

        largeIconBitmap?.let {
            builder.setLargeIcon(it)
        }

        // Se c'è una proposta di incontro, aggiungi l'azione 1-Tap per accettarla direttamente
        if (email.hasAppointmentProposal && email.suggestedSlot.isNotBlank()) {
            val acceptMeetingIntent = Intent(context, EmailActionReceiver::class.java).apply {
                action = ACTION_ACCEPT_MEETING
                putExtra(EXTRA_EMAIL_ID, email.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MEETING_TITLE, "Meeting: ${email.subject}")
                putExtra(EXTRA_MEETING_SLOT, email.suggestedSlot)
                putExtra(EXTRA_MEETING_DURATION, email.suggestedDurationMinutes)
                putExtra(EXTRA_MEETING_CONTACT, cleanSender)
            }
            val acceptMeetingPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                acceptMeetingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_menu_my_calendar,
                "Accetta Incontro 📅",
                acceptMeetingPendingIntent
            )
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId)
    }
}
