package com.example.assistentemeditechmobile.data

import android.content.Context
import com.example.assistentemeditechmobile.domain.EmailAccountConfig
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
        isLenient = true
    }

    private val mutex = Mutex()
    private var cachedEmails: List<EmailItem>? = null
    private var cachedConfig: EmailAccountConfig? = null

    private val emailsFile: File
        get() = File(context.filesDir, "meditech_emails.json")

    private val configFile: File
        get() = File(context.filesDir, "meditech_email_config.json")

    private val defaultEmails = listOf(
        EmailItem(
            id = "em-1",
            accountName = "SUPSI MeDiTech",
            sender = "Prof. Stéphane Meystre <stephane.meystre@supsi.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Allineamento su Progetto Clinico AI & Cartelle Elettroniche",
            dateStr = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            bodySnippet = "Caro Manuele, vorrei fare un breve allineamento di 30 minuti domani alle 14:00 riguardo l'avanzamento dei moduli NLP clinici per MeDiTech. Fammi sapere se l'orario ti va bene o proponi un'alternativa.",
            summary = "Il Prof. Stéphane Meystre propone un incontro di 30 minuti domani alle 14:00 per discutere l'avanzamento dei moduli NLP clinici per MeDiTech.",
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
            summary = "Feedback positivo da EOC Ticino sulla conformità del dataset clinico anonimizzato al protocollo etico approvato.",
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
            summary = "Newsletter mensile IEEE su innovazioni nell'AI medica e biosensori (archiviata automaticamente).",
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
            getEmailsInternal()
        }
    }

    suspend fun getEmailAccountConfig(): EmailAccountConfig = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedConfig?.let { return@withLock it }
            if (configFile.exists()) {
                runCatching {
                    val config = json.decodeFromString<EmailAccountConfig>(configFile.readText())
                    cachedConfig = config
                    config
                }.getOrElse {
                    val defaultConfig = EmailAccountConfig()
                    configFile.writeText(json.encodeToString(defaultConfig))
                    cachedConfig = defaultConfig
                    defaultConfig
                }
            } else {
                val defaultConfig = EmailAccountConfig()
                configFile.writeText(json.encodeToString(defaultConfig))
                cachedConfig = defaultConfig
                defaultConfig
            }
        }
    }

    suspend fun saveEmailAccountConfig(config: EmailAccountConfig) = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedConfig = config
            configFile.writeText(json.encodeToString(config))
        }
    }

    suspend fun testConnection(config: EmailAccountConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Validazione parametri
            if (config.emailAddress.isBlank() || !config.emailAddress.contains("@")) {
                error("Indirizzo email non valido")
            }
            if (config.imapHost.isBlank()) {
                error("Host server IMAP mancante")
            }
            if (config.smtpHost.isBlank()) {
                error("Host server SMTP mancante")
            }
            // Simula hand-shake crittografato SSL/TLS
            kotlinx.coroutines.delay(650)
            "Connessione stabilita con successo: IMAP SSL (porta ${config.imapPort}) e SMTP STARTTLS (porta ${config.smtpPort}) attivi."
        }
    }

    suspend fun simulateIncomingEmail(): EmailItem = withContext(Dispatchers.IO) {
        mutex.withLock {
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            val newMail = EmailItem(
                id = "em-" + UUID.randomUUID().toString().take(6),
                accountName = cachedConfig?.provider ?: "SUPSI MeDiTech",
                sender = "Dr. Luca Bernasconi <luca.bernasconi@cardiocentro.org>",
                recipient = cachedConfig?.emailAddress ?: "manuele@supsi.ch",
                subject = "Richiesta Urgente: Valutazione Tracciati ECG con Algoritmo AI",
                dateStr = nowStr,
                bodySnippet = "Gentile Manuele, abbiamo un lotto di tracciati complessi da validare con il modello neurale. Riusciamo a fare un punto rapido oggi alle 16:30 o domani alle 09:30? Ti allego l'estratto anonimizzato.",
                summary = "Il Dr. Luca Bernasconi (Cardiocentro) richiede una valutazione urgente di tracciati ECG e propone un incontro oggi alle 16:30 o domani alle 09:30.",
                language = "it",
                isImportant = true,
                isVip = true,
                hasAppointmentProposal = true,
                suggestedSlot = "Oggi alle 16:30",
                suggestedDurationMinutes = 45,
                draftReply = "Gentile Dr. Bernasconi,\n\nho analizzato la richiesta: confermo la disponibilità per l'incontro di oggi alle ore 16:30. Ho inserito l'appuntamento in agenda con alert clinico attivo.\n\nA presto,\nManuele",
                attachments = listOf("Estratto_ECG_Cardiocentro_04.pdf"),
                isRead = false,
                timestampMillis = System.currentTimeMillis()
            )

            val current = getEmailsInternal().toMutableList()
            current.add(0, newMail)
            cachedEmails = current
            emailsFile.writeText(json.encodeToString(current))
            newMail
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
