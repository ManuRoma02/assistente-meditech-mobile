package com.example.assistentemeditechmobile.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.assistentemeditechmobile.R
import com.example.assistentemeditechmobile.data.CalendarRepository
import com.example.assistentemeditechmobile.data.EmailRepository
import com.example.assistentemeditechmobile.domain.MeditechAiEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val emailId = intent.getStringExtra(EmailNotificationManager.EXTRA_EMAIL_ID) ?: return
        val notifId = intent.getIntExtra(EmailNotificationManager.EXTRA_NOTIFICATION_ID, emailId.hashCode())

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vipRepo = com.example.assistentemeditechmobile.data.VipRepository(context)
                val emailRepo = EmailRepository(context, vipRepo)

                when (action) {
                    EmailNotificationManager.ACTION_MARK_READ -> {
                        emailRepo.markAsRead(emailId)
                        EmailNotificationManager.dismissNotification(context, notifId)
                    }

                    EmailNotificationManager.ACTION_ACCEPT_MEETING -> {
                        val title = intent.getStringExtra(EmailNotificationManager.EXTRA_MEETING_TITLE) ?: "Meeting MeDiTech"
                        val slot = intent.getStringExtra(EmailNotificationManager.EXTRA_MEETING_SLOT) ?: ""
                        val contact = intent.getStringExtra(EmailNotificationManager.EXTRA_MEETING_CONTACT) ?: "Contatto Clinico"
                        val duration = intent.getIntExtra(EmailNotificationManager.EXTRA_MEETING_DURATION, 30)

                        val aiEngine = MeditechAiEngine()
                        val prompt = if (slot.isNotBlank()) "fissa meeting $title con $contact $slot" else "fissa meeting $title con $contact"
                        val parsed = aiEngine.parseAppointmentInstruction(prompt).let {
                            if (duration > 0) it.copy(durata_minuti = duration) else it
                        }

                        val calendarRepo = CalendarRepository(context)
                        calendarRepo.createEvent(
                            proposed = parsed,
                            bufferMinutes = 15L,
                            autoCreateMeet = true
                        )

                        // Segna anche come letta
                        emailRepo.markAsRead(emailId)

                        // Notifica di conferma aggiornata
                        val confirmNotif = NotificationCompat.Builder(context, EmailNotificationManager.CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_stat_email_recap)
                            .setContentTitle("✓ Incontro Confermato in Agenda Clinica")
                            .setContentText("$title programmato per $slot")
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .setBigContentTitle("✓ Incontro Confermato in Agenda Clinica")
                                    .bigText("L'incontro con **$contact** è stato inserito con successo nell'Agenda protetta.\nSlot confermato: $slot ($duration min).")
                            )
                            .setColor(0xFF009688.toInt())
                            .setAutoCancel(true)
                            .setTimeoutAfter(6000)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build()

                        NotificationManagerCompat.from(context).notify(notifId, confirmNotif)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
