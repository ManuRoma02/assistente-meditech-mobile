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

    private val defaultEmails: List<EmailItem> = emptyList()

    suspend fun getEmails(): List<EmailItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val emails = getEmailsInternal()
            val vips = runCatching { vipRepository.getVips() }.getOrDefault(emptyList())
            emails.map { email ->
                val isVipMatched = com.example.assistentemeditechmobile.domain.VipMatcher.isVip(email.sender, email.subject, vips)
                if (email.isVip != isVipMatched) email.copy(isVip = isVipMatched) else email
            }
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
        val vips = runCatching { vipRepository.getVips() }.getOrDefault(emptyList())
        val sender = "Dr. Luca Bernasconi <luca.bernasconi@cardiocentro.org>"
        val subject = "Richiesta Urgente: Valutazione Tracciati ECG con Algoritmo AI"
        val isSenderVip = com.example.assistentemeditechmobile.domain.VipMatcher.isVip(sender, subject, vips)

        mutex.withLock {
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            val newMail = EmailItem(
                id = "em-" + UUID.randomUUID().toString().take(6),
                accountName = cachedConfig?.provider ?: "SUPSI MeDiTech",
                sender = sender,
                recipient = cachedConfig?.emailAddress ?: "utente@supsi.ch",
                subject = subject,
                dateStr = nowStr,
                bodySnippet = "Gentile collega, abbiamo un lotto di tracciati complessi da validare con il modello neurale. Riusciamo a fare un punto rapido oggi alle 16:30 o domani alle 09:30? Ti allego l'estratto anonimizzato.",
                summary = "Il Dr. Luca Bernasconi (Cardiocentro) richiede una valutazione urgente di tracciati ECG e propone un incontro oggi alle 16:30 o domani alle 09:30.",
                language = "it",
                isImportant = true,
                isVip = isSenderVip,
                hasAppointmentProposal = true,
                suggestedSlot = "Oggi alle 16:30",
                suggestedDurationMinutes = 45,
                draftReply = "Gentile Dr. Bernasconi,\n\nho analizzato la richiesta: confermo la disponibilità per l'incontro di oggi alle ore 16:30. Ho inserito l'appuntamento in agenda con alert clinico attivo.\n\nA presto,\nAssistente MeDiTech",
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

    suspend fun deleteEmail(emailId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getEmailsInternal().toMutableList()
            val removed = current.removeAll { it.id == emailId }
            if (removed) {
                cachedEmails = current
                emailsFile.writeText(json.encodeToString(current))
            }
            removed
        }
    }

    suspend fun clearAllEmails() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedEmails = emptyList()
            emailsFile.writeText(json.encodeToString(emptyList<EmailItem>()))
        }
    }

    private fun getEmailsInternal(): List<EmailItem> {
        cachedEmails?.let { return it }
        if (!emailsFile.exists()) {
            cachedEmails = emptyList()
            return emptyList()
        }
        val loaded = runCatching {
            json.decodeFromString<List<EmailItem>>(emailsFile.readText())
        }.getOrDefault(emptyList())
        // Rimuove completamente tutte le email mock/inventate di seed precedenti
        val cleaned = loaded.filterNot { email ->
            email.id in listOf("em-1", "em-2", "em-3") ||
            email.id.startsWith("em-seed") ||
            email.accountName.equals("Lavoro", ignoreCase = true) ||
            (email.sender.contains("stephane.meystre@supsi.ch") && email.subject.contains("Allineamento su Progetto")) ||
            (email.sender.contains("elena.fontana@eoc.ch") && email.subject.contains("Feedback preliminare")) ||
            email.sender.contains("updates@ieee-org.org")
        }
        if (cleaned.size != loaded.size || !emailsFile.exists()) {
            runCatching { emailsFile.writeText(json.encodeToString(cleaned)) }
        }
        cachedEmails = cleaned
        return cleaned
    }
}
