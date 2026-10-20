package com.example.assistentemeditechmobile.data

import android.content.Context
import com.example.assistentemeditechmobile.domain.EmailItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class EmailRepository(
    private val context: Context,
    private val vipRepository: VipRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private var cachedEmails: List<EmailItem>? = null
    private val emailsFile: File
        get() = File(context.filesDir, "meditech_emails.json")

    private val defaultEmails = listOf(
        EmailItem(
            id = "em-1",
            accountName = "SUPSI MeDiTech",
            sender = "Prof. Stéphane Meystre <stephane.meystre@supsi.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Allineamento su Progetto Clinico AI & Cartelle Elettroniche",
            dateStr = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            bodySnippet = "Caro Manuele, vorrei fare un breve allineamento di 30 minuti domani alle 14:00 riguardo l'avanzamento dei moduli NLP clinici per MeDiTech. Fammi sapere se l'orario ti va bene o proponi un'alternativa.",
            summary = "Il Prof. Stéphane Meystre propone un allineamento di 30 minuti per domani alle 14:00 sullo sviluppo dei moduli NLP clinici per MeDiTech.",
            language = "it",
            isImportant = true,
            isVip = true,
            hasAppointmentProposal = true,
            suggestedSlot = "Domani alle 14:00",
            suggestedDurationMinutes = 30,
            draftReply = "Gentile Prof. Meystre,\n\nti confermo la piena disponibilità per il meeting di domani alle ore 14:00. Ho già provveduto a registrare l'appuntamento in agenda con promemoria attivo.\n\nA domani e cordiali saluti,\nManuele",
            attachments = listOf("Avanzamento_MeDiTech_NLP.pdf"),
            isRead = false
        ),
        EmailItem(
            id = "em-2",
            accountName = "SUPSI MeDiTech",
            sender = "Dr. Elena Fontana <elena.fontana@eoc.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Feedback preliminare su validazione dataset clinico",
            dateStr = LocalDateTime.now().minusHours(5).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            bodySnippet = "Gentile collega, abbiamo revisionato il primo set di dati anonimizzati dell'ospedale. I parametri di accuratezza sono in linea con il protocollo etico approvato.",
            summary = "Feedback positivo da EOC Ticino sulla conformità del primo set di dati clinici anonimizzati rispetto al protocollo etico approvato.",
            language = "it",
            isImportant = true,
            isVip = true,
            hasAppointmentProposal = false,
            suggestedSlot = "",
            suggestedDurationMinutes = 0,
            draftReply = "Gentile Dr.ssa Fontana,\n\nti ringrazio per l'aggiornamento e per l'attenta revisione. Procediamo come concordato con la seconda fase di validazione.\n\nCordiali saluti,\nManuele",
            attachments = listOf("Report_Etico_EOC_V1.pdf"),
            isRead = true
        ),
        EmailItem(
            id = "em-3",
            accountName = "Lavoro",
            sender = "IEEE Healthcare Tech <updates@ieee-org.org>",
            recipient = "manuele@supsi.ch",
            subject = "Monthly Newsletter: Digital Health and Medical AI Innovations",
            dateStr = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            bodySnippet = "Explore the latest breakthroughs in clinical decision support systems, wearable biosensors, and regulatory compliance for medical device software.",
            summary = "Newsletter mensile IEEE su innovazioni nell'AI medica e biosensori (filtrata automaticamente come bulk).",
            language = "en",
            isImportant = false,
            isVip = false,
            hasAppointmentProposal = false,
            suggestedSlot = "",
            suggestedDurationMinutes = 0,
            draftReply = "",
            attachments = emptyList(),
            isRead = true
        )
    )

    suspend fun getEmails(): List<EmailItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!emailsFile.exists()) {
                emailsFile.writeText(json.encodeToString(defaultEmails))
                return@withLock defaultEmails
            }
            runCatching {
                json.decodeFromString<List<EmailItem>>(emailsFile.readText())
            }.getOrElse {
                emailsFile.writeText(json.encodeToString(defaultEmails))
                defaultEmails
            }
        }
    }

    suspend fun markAsRead(emailId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getEmailsInternal().toMutableList()
            val idx = current.indexOfFirst { it.id == emailId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(isRead = true)
                cachedEmails = current
                emailsFile.writeText(json.encodeToString(current))
            }
        }
    }

    suspend fun updateDraftReply(emailId: String, newDraft: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getEmailsInternal().toMutableList()
            val idx = current.indexOfFirst { it.id == emailId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(draftReply = newDraft)
                cachedEmails = current
                emailsFile.writeText(json.encodeToString(current))
            }
        }
    }

    suspend fun generateAiDraft(emailItem: EmailItem, userInstruction: String = ""): String = withContext(Dispatchers.Default) {
        com.example.assistentemeditechmobile.domain.MeditechDraftGenerator.generate(emailItem, userInstruction)
    }

    suspend fun sendEmailReply(emailItem: EmailItem, replyBody: String): Boolean = withContext(Dispatchers.IO) {
        // Simulates instant SMTP dispatch with local status confirmation
        markAsRead(emailItem.id)
        true
    }

    private fun getEmailsInternal(): List<EmailItem> {
        cachedEmails?.let { return it }
        if (!emailsFile.exists()) {
            cachedEmails = defaultEmails
            return defaultEmails
        }
        val loaded = runCatching {
            json.decodeFromString<List<EmailItem>>(emailsFile.readText())
        }.getOrDefault(defaultEmails)
        cachedEmails = loaded
        return loaded
    }
}
