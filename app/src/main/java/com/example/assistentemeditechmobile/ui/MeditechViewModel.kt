package com.example.assistentemeditechmobile.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistentemeditechmobile.data.CalendarRepository
import com.example.assistentemeditechmobile.data.ClinicalProjectRepository
import com.example.assistentemeditechmobile.data.EmailRepository
import com.example.assistentemeditechmobile.data.VipRepository
import com.example.assistentemeditechmobile.data.ai.OnDeviceGenerationOptions
import com.example.assistentemeditechmobile.data.ai.OnDeviceLocalAiClient
import com.example.assistentemeditechmobile.domain.AppointmentParseResult
import com.example.assistentemeditechmobile.domain.CalendarEventItem
import com.example.assistentemeditechmobile.domain.ChatMessage
import com.example.assistentemeditechmobile.domain.ClinicalProject
import com.example.assistentemeditechmobile.domain.EmailAccountConfig
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.domain.MeditechAiEngine
import com.example.assistentemeditechmobile.domain.MeditechEasterEggs
import com.example.assistentemeditechmobile.domain.MeditechTab
import com.example.assistentemeditechmobile.domain.VipContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class MeditechUiState(
    val currentTab: MeditechTab = MeditechTab.CHAT_AI,
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<CalendarEventItem> = emptyList(),
    val emails: List<EmailItem> = emptyList(),
    val emailAccountConfig: EmailAccountConfig = EmailAccountConfig(),
    val showEmailConfigSheet: Boolean = false,
    val selectedEmailDetail: EmailItem? = null,
    val projects: List<ClinicalProject> = emptyList(),
    val vips: List<VipContact> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val isVoiceListening: Boolean = false,
    val isAiProcessing: Boolean = false,
    val statusBanner: String? = null,
    val isUpdateAvailable: Boolean = false,
    val updateManifest: com.example.assistentemeditechmobile.data.ota.OtaUpdateManifest? = null,
    val isDownloadingUpdate: Boolean = false,
    val updateProgress: Float = 0f,
    val readyToInstallApk: java.io.File? = null,
    val selectedAiModel: String = OnDeviceLocalAiClient.DEFAULT_MODEL_NAME,
    val onDeviceModelPath: String = OnDeviceLocalAiClient.DEFAULT_WEIGHTS_PATH,
    val onDeviceModelStatus: String? = null,
    val onDeviceQuantization: String = "4-bit (Q4_K_M)",
    val onDevicePrivacyMode: String = "100% Locale • Zero Cloud",
    val responseStyle: String = "Clinico Bilanciato",
    val autoCreateMeet: Boolean = true,
    val safetyBufferEnabled: Boolean = true,
    val showSettingsDialog: Boolean = false,
    val showProjectsSheet: Boolean = false,
    val isClearingChat: Boolean = false,
    val deletingEventIds: Set<String> = emptySet(),
    val sentEmailIds: Set<String> = emptySet(),
    val easterEggMessage: String? = null,
    val incomingEmailAlert: EmailItem? = null
)

class MeditechViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val vipRepository = VipRepository(context)
    val calendarRepository = CalendarRepository(context)
    val emailRepository = EmailRepository(context, vipRepository)
    val projectRepository = ClinicalProjectRepository(context)
    val aiEngine = MeditechAiEngine(calendarRepository, vipRepository)
    val onDeviceAiClient = OnDeviceLocalAiClient()
    val otaUpdateManager = com.example.assistentemeditechmobile.data.ota.OtaUpdateManager(context)

    private val easterEggPreferences = context.getSharedPreferences("meditech_easter_eggs", Context.MODE_PRIVATE)
    private val lastEasterEggIndexKey = "last_message_index"

    private val _uiState = MutableStateFlow(MeditechUiState())
    val uiState: StateFlow<MeditechUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        checkForAppUpdate()
    }

    fun refreshEasterEggForAppOpen() {
        val messages = MeditechEasterEggs.messages
        if (messages.isEmpty()) return

        val previousIndex = easterEggPreferences.getInt(lastEasterEggIndexKey, -1)
        val candidates = messages.indices.filter { it != previousIndex }.ifEmpty { messages.indices.toList() }
        val nextIndex = candidates.random()
        easterEggPreferences.edit().putInt(lastEasterEggIndexKey, nextIndex).apply()
        _uiState.update { it.copy(easterEggMessage = messages[nextIndex]) }
    }

    fun dismissEasterEgg() {
        _uiState.update { it.copy(easterEggMessage = null) }
    }

    fun selectTab(tab: MeditechTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        refreshEvents()
    }

    fun setEmailConfigSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showEmailConfigSheet = visible)
    }

    fun setProjectsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showProjectsSheet = visible) }
    }

    fun openEmailDetail(email: EmailItem) {
        viewModelScope.launch {
            emailRepository.markAsRead(email.id)
            val updated = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                emails = updated,
                selectedEmailDetail = email.copy(isRead = true)
            )
        }
    }

    fun dismissEmailDetail() {
        _uiState.value = _uiState.value.copy(selectedEmailDetail = null)
    }

    fun deleteEmail(emailId: String) {
        viewModelScope.launch {
            emailRepository.deleteEmail(emailId)
            val updated = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                emails = updated,
                statusBanner = "Email rimossa dalla casella",
                selectedEmailDetail = if (_uiState.value.selectedEmailDetail?.id == emailId) null else _uiState.value.selectedEmailDetail
            )
        }
    }

    fun clearAllEmails() {
        viewModelScope.launch {
            emailRepository.clearAllEmails()
            _uiState.value = _uiState.value.copy(
                emails = emptyList(),
                statusBanner = "Tutte le email sono state rimosse",
                selectedEmailDetail = null
            )
        }
    }

    fun saveEmailAccountConfig(config: EmailAccountConfig) {
        viewModelScope.launch {
            emailRepository.saveEmailAccountConfig(config)
            _uiState.value = _uiState.value.copy(
                emailAccountConfig = config,
                statusBanner = "Configurazione email '${config.emailAddress}' salvata con successo"
            )
        }
    }

    suspend fun testEmailConnection(config: EmailAccountConfig): Result<String> {
        return emailRepository.testConnection(config)
    }

    fun openEmailById(emailId: String) {
        viewModelScope.launch {
            val email = emailRepository.getEmails().firstOrNull { it.id == emailId }
            if (email != null) {
                openEmailDetail(email)
                selectTab(MeditechTab.EMAIL_HUB)
            }
        }
    }

    fun dismissIncomingEmailAlert() {
        _uiState.value = _uiState.value.copy(incomingEmailAlert = null)
    }

    fun simulateReceiveIncomingEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true)
            val newMail = emailRepository.simulateIncomingEmail()
            val updatedEmails = emailRepository.getEmails()

            // Genera Notifica Android ad Alta Priorità con Sintesi On-Device
            com.example.assistentemeditechmobile.data.notification.EmailNotificationManager.showEmailNotification(context, newMail)

            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                emails = updatedEmails,
                incomingEmailAlert = newMail,
                statusBanner = "Nuova email ricevuta da ${newMail.sender.substringBefore('<').trim()}: Notifica con Recap inviata!"
            )
        }
    }

    fun triggerTestEmailNotification() {
        simulateReceiveIncomingEmail()
    }

    fun addMeetingFromEmail(title: String, slotStr: String, durationMinutes: Int, contact: String) {
        viewModelScope.launch {
            val prompt = if (slotStr.isNotBlank()) "fissa meeting $title con $contact $slotStr" else "fissa meeting $title con $contact"
            val parsed = aiEngine.parseAppointmentInstruction(prompt).let {
                if (durationMinutes > 0) it.copy(durata_minuti = durationMinutes) else it
            }
            val buffer = if (_uiState.value.safetyBufferEnabled) 15L else 0L
            val created = calendarRepository.createEvent(
                proposed = parsed,
                bufferMinutes = buffer,
                autoCreateMeet = _uiState.value.autoCreateMeet
            )
            refreshEvents()
            _uiState.value = _uiState.value.copy(
                statusBanner = if (created.hasConflict) {
                    "Meeting con '$contact' aggiunto con avviso di conflitto!"
                } else {
                    "Meeting con '$contact' aggiunto all'Agenda Clinica!"
                }
            )
        }
    }

    fun generateEmailExecutiveBriefing(emailsList: List<EmailItem> = _uiState.value.emails): String {
        val count = emailsList.size
        val vips = emailsList.filter { it.isVip }

        return buildString {
            appendLine("📬 **Briefing Email Cliniche ($count messaggi, ${vips.size} prioritari VIP):**")
            appendLine()
            val targetList = if (vips.isNotEmpty()) vips.take(3) else emailsList.take(3)
            for (mail in targetList) {
                val senderClean = mail.sender.substringBefore('<').trim()
                val meetingBadge = if (mail.hasAppointmentProposal) " • *Richiesta Incontro*" else ""
                appendLine("• **$senderClean**: ${mail.subject}$meetingBadge")
            }
            appendLine()
            append("💡 *Tocca 'Email' nella barra in basso per leggere i testi o inviare risposte 1-Tap.*")
        }
    }

    fun postEmailBriefingToChat() {
        viewModelScope.launch {
            val briefing = generateEmailExecutiveBriefing()
            val msg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = briefing
            )
            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages + msg,
                statusBanner = "Resoconto email cliniche aggiornato"
            )
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val vips = vipRepository.getVips()
            val emails = emailRepository.getEmails()
            val config = emailRepository.getEmailAccountConfig()
            val projects = projectRepository.getProjects()
            val events = calendarRepository.getEventsForDate(_uiState.value.selectedDate)

            val welcomeMsg = ChatMessage(
                id = "init-msg",
                role = "assistant",
                text = "🏥 **Assistente Clinico MeDiTech attivo (100% On-Device).**\n\n" +
                    "• 📅 **/agenda** — Impegni e visite cliniche di oggi\n" +
                    "• 📝 **/fissa** — Pianifica incontro con controllo conflitti\n" +
                    "• 🔄 **/sposta** o **/cancella** — Gestione rapida appuntamenti\n" +
                    "• 📬 **/email** — Sintesi comunicazioni VIP Prof. Meystre\n" +
                    "• 🔬 **/progetti** — Schede e telemetria dossier di ricerca\n" +
                    "• 💡 Digita **/** per l'elenco comandi con sintesi rapida"
            )

            _uiState.value = _uiState.value.copy(
                vips = vips,
                emails = emails,
                emailAccountConfig = config,
                projects = projects,
                events = events,
                chatMessages = listOf(welcomeMsg)
            )
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            val events = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
            _uiState.value = _uiState.value.copy(events = events)
        }
    }

    fun createAppointmentFromInstruction(instruction: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true)
            val parsed = aiEngine.parseAppointmentInstruction(instruction)
            val buffer = if (_uiState.value.safetyBufferEnabled) 15L else 0L
            val created = calendarRepository.createEvent(
                proposed = parsed,
                bufferMinutes = buffer,
                autoCreateMeet = _uiState.value.autoCreateMeet
            )
            refreshEvents()
            _uiState.value = _uiState.value.copy(isAiProcessing = false)

            val confirmationText = if (created.hasConflict) {
                "⚠️ **Appuntamento salvato con sovrapposizione d'orario.**\nVerifica il conflitto rilevato nella scheda sottostante."
            } else {
                "✅ **Appuntamento confermato in agenda clinica.**"
            }

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = confirmationText,
                appointmentProposal = parsed,
                createdEvent = created
            )
            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages + assistantMsg,
                statusBanner = "Appuntamento '${created.title}' registrato con successo"
            )
        }
    }

    fun clearChatHistory(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearingChat = true)
            kotlinx.coroutines.delay(450L)
            val welcomeMsg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = "🧹 **Cronologia chat reimpostata.**\n\n" +
                    "• 📅 Scrivi *'/agenda'* o *'impegni di oggi'* per consultare le visite\n" +
                    "• 📬 Scrivi *'/email'* o *'briefing'* per il riassunto comunicazioni VIP\n" +
                    "• 🔬 Scrivi *'/progetti'* per lo stato delle ricerche MeDiTech"
            )
            _uiState.value = _uiState.value.copy(
                chatMessages = listOf(welcomeMsg),
                isClearingChat = false,
                statusBanner = "Cronologia chat ripulita"
            )
            onComplete()
        }
    }

    fun cancelAppointment(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingEventIds = _uiState.value.deletingEventIds + eventId
            )
            kotlinx.coroutines.delay(350L)
            val ok = calendarRepository.cancelEvent(eventId)
            if (ok) {
                refreshEvents()
                _uiState.value = _uiState.value.copy(
                    deletingEventIds = _uiState.value.deletingEventIds - eventId,
                    statusBanner = "Appuntamento annullato con successo"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    deletingEventIds = _uiState.value.deletingEventIds - eventId
                )
            }
        }
    }

    fun rescheduleAppointmentFromInstruction(instruction: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true)
            val parsed = aiEngine.parseAppointmentInstruction(instruction)
            val currentEvents = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
            val lowerInst = instruction.lowercase()
            val target = currentEvents.firstOrNull { ev ->
                lowerInst.contains(ev.title.lowercase()) || (ev.contact.isNotBlank() && lowerInst.contains(ev.contact.lowercase().substringBefore(" ")))
            } ?: currentEvents.firstOrNull()

            if (target != null && parsed.data_inizio_iso != null) {
                val updated = calendarRepository.rescheduleEvent(
                    eventId = target.id,
                    newStartIso = parsed.data_inizio_iso,
                    newDurationMinutes = parsed.durata_minuti
                )
                refreshEvents()
                _uiState.value = _uiState.value.copy(isAiProcessing = false)
                val newTime = parsed.data_inizio_iso.drop(11).take(5)
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString().take(8),
                    role = "assistant",
                    text = "🔄 **Appuntamento riprogrammato:** '${target.title}' spostato alle $newTime.",
                    appointmentProposal = parsed,
                    createdEvent = updated ?: target
                )
                _uiState.value = _uiState.value.copy(
                    chatMessages = _uiState.value.chatMessages + assistantMsg,
                    statusBanner = "Appuntamento '${target.title}' spostato alle $newTime"
                )
            } else {
                _uiState.value = _uiState.value.copy(isAiProcessing = false)
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString().take(8),
                    role = "assistant",
                    text = "⚠️ **Nessun appuntamento trovato da riprogrammare.** Digita /agenda per verificare le visite attive."
                )
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
            }
        }
    }

    fun deleteOrCancelAppointmentFromQuery(query: String) {
        viewModelScope.launch {
            val evs = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
            if (evs.isEmpty()) {
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString().take(8),
                    role = "assistant",
                    text = "ℹ️ **Nessun appuntamento da cancellare nella data selezionata.**"
                )
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                return@launch
            }
            val target = if (query.isBlank()) {
                evs.first()
            } else {
                evs.firstOrNull {
                    it.title.contains(query, ignoreCase = true) ||
                        it.contact.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true)
                } ?: evs.first()
            }
            cancelAppointment(target.id)
            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = "🗑️ **Appuntamento annullato:** '${target.title}' rimosso dall'agenda clinica."
            )
            _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
        }
    }

    fun generateReplyDraft(emailItem: EmailItem, userInstruction: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true)
            val modelName = _uiState.value.selectedAiModel
            val isEnglish = emailItem.language.equals("en", ignoreCase = true)

            val localResult = onDeviceAiClient.generateEmailReplyDraft(
                modelName = modelName,
                sender = emailItem.sender,
                subject = emailItem.subject,
                bodySnippet = emailItem.bodySnippet,
                userInstruction = userInstruction,
                isEnglishEmail = isEnglish
            )

            val draft = localResult.getOrElse {
                emailRepository.generateAiDraft(emailItem, userInstruction)
            }
            emailRepository.updateDraftReply(emailItem.id, draft)
            val updated = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                emails = updated,
                statusBanner = "Bozza Qwen 3.5 2B (On-Device 4-bit) predisposta per ${emailItem.sender.substringBefore('<').trim()}"
            )
        }
    }

    fun sendEmailReply(emailItem: EmailItem, replyText: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sentEmailIds = _uiState.value.sentEmailIds + emailItem.id
            )
            kotlinx.coroutines.delay(450L)
            emailRepository.sendEmailReply(emailItem, replyText)
            val updated = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                emails = updated,
                statusBanner = "Risposta inviata via email con successo"
            )
        }
    }

    fun addVipContact(name: String, emailOrDomain: String, role: String) {
        viewModelScope.launch {
            vipRepository.addVip(name, emailOrDomain, role)
            val vips = vipRepository.getVips()
            _uiState.value = _uiState.value.copy(
                vips = vips,
                statusBanner = "Contatto prioritario '$name' aggiunto all'elenco VIP"
            )
        }
    }

    fun removeVipContact(vipId: String) {
        viewModelScope.launch {
            vipRepository.removeVip(vipId)
            val vips = vipRepository.getVips()
            _uiState.value = _uiState.value.copy(vips = vips)
        }
    }

    fun sendUserChatMessage(text: String) {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString().take(8),
            role = "user",
            text = text
        )
        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + userMsg)

        val clean = text.trim()
        val lower = clean.lowercase()

        // 0. ROUTING DETERMINISTICO COMANDI SLASH (/agenda, /fissa, /email, /progetti, /aggiorna, /aiuto)
        if (clean.startsWith("/")) {
            when {
                lower == "/oggi" || lower == "/agenda" || lower.startsWith("/agenda ") -> {
                    viewModelScope.launch {
                        val evs = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
                        val reply = if (evs.isEmpty()) {
                            "📅 **Agenda di Oggi:** Nessun impegno clinico in programma."
                        } else {
                            buildString {
                                appendLine("📅 **Agenda Clinica di Oggi (${evs.size} impegni):**")
                                for (ev in evs) {
                                    val startH = ev.startIso.drop(11).take(5)
                                    val endH = ev.endIso.drop(11).take(5)
                                    appendLine("• **$startH - $endH** | **${ev.title}** (${ev.location})")
                                }
                            }.trimEnd()
                        }
                        val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = reply)
                        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    }
                    return
                }
                lower == "/fissa" || lower.startsWith("/fissa ") -> {
                    val instruction = clean.removePrefix("/fissa").trim()
                    if (instruction.isBlank()) {
                        val assistantMsg = ChatMessage(
                            id = UUID.randomUUID().toString().take(8),
                            role = "assistant",
                            text = "📅 **Fissa un nuovo incontro MeDiTech**\n\nScrivi ad esempio:\n• */fissa domani alle 14 con Stéphane Meystre di 30 min*\n• */fissa venerdì alle 10 meeting clinico con Dr.ssa Fontana*"
                        )
                        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    } else {
                        createAppointmentFromInstruction(instruction)
                    }
                    return
                }
                lower == "/sposta" || lower.startsWith("/sposta ") || lower == "/rinvia" || lower.startsWith("/rinvia ") -> {
                    val instruction = clean.replaceFirst(Regex("^/(?:sposta|rinvia)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    if (instruction.isBlank()) {
                        val assistantMsg = ChatMessage(
                            id = UUID.randomUUID().toString().take(8),
                            role = "assistant",
                            text = "🔄 **Sposta Appuntamento:** Specifica orario o giorno.\nEs: */sposta alle 16* oppure */sposta visita domani alle 15*"
                        )
                        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    } else {
                        rescheduleAppointmentFromInstruction(instruction)
                    }
                    return
                }
                lower == "/cancella" || lower.startsWith("/cancella ") || lower == "/elimina" || lower.startsWith("/elimina ") -> {
                    val query = clean.replaceFirst(Regex("^/(?:cancella|elimina)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    deleteOrCancelAppointmentFromQuery(query)
                    return
                }
                lower == "/email" || lower == "/briefing" || lower == "/posta" -> {
                    viewModelScope.launch {
                        val briefing = generateEmailExecutiveBriefing()
                        val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = briefing)
                        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    }
                    return
                }
                lower == "/progetti" || lower == "/ricerca" || lower == "/dossier" -> {
                    setProjectsSheetVisible(true)
                    viewModelScope.launch {
                        val projects = projectRepository.getProjects()
                        val reply = buildString {
                            appendLine("🔬 **Dossier Ricerca MeDiTech (${projects.size} attivi):**")
                            for (p in projects.take(3)) {
                                appendLine("• **${p.codice}** — **${p.titolo}** (${p.stato})")
                            }
                        }.trimEnd()
                        val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = reply)
                        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    }
                    return
                }
                lower == "/aggiorna" || lower == "/update" -> {
                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString().take(8),
                        role = "assistant",
                        text = "🔄 **Controllo Aggiornamenti In-App...**\nVerifico se è disponibile una nuova versione per l'app installata."
                    )
                    _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    checkForAppUpdate(force = true)
                    return
                }
                lower == "/impostazioni" || lower == "/settings" || lower == "/modelli" -> {
                    setSettingsDialogVisible(true)
                    return
                }
                lower == "/pulisci" || lower == "/clear" -> {
                    clearChatHistory()
                    return
                }
                lower == "/aiuto" || lower == "/help" || lower == "/?" -> {
                    val helpText = """⚡ **Comandi Rapidi MeDiTech (Slash Commands)**

• 📅 **/agenda**: Mostra gli impegni clinici della giornata
• 📝 **/fissa [dettagli]**: Fissa una visita o riunione (es: `/fissa domani alle 15`)
• 🔄 **/sposta [dettagli]**: Riprogramma un appuntamento esistente (es: `/sposta alle 16`)
• 🗑️ **/cancella [titolo]**: Rimuove un appuntamento dall'agenda clinica
• 📬 **/email**: Sintesi concisa delle comunicazioni prioritarie VIP
• 🔬 **/progetti**: Apre i dossier e lo stato delle ricerche SUPSI & EOC
• 🔄 **/aggiorna**: Verifica e installa aggiornamenti OTA da Git
• ⚙️ **/impostazioni**: Configurazione modello IA locale e preferenze
• 🧹 **/pulisci**: Ripulisce la cronologia dei messaggi chat"""
                    val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = helpText)
                    _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
                    return
                }
            }
        }

        // 1. ROUTING IN LINGUAGGIO NATURALE
        if (lower.startsWith("sposta") || lower.startsWith("rinvia") || lower.contains("riprogramma")) {
            rescheduleAppointmentFromInstruction(text)
        } else if (lower.startsWith("cancella") || lower.startsWith("elimina") || lower.startsWith("annulla appuntamento")) {
            val q = text.replaceFirst(Regex("^(?:cancella|elimina|annulla)\\s+(?:l'appuntamento\\s+|l'evento\\s+|la\\s+|il\\s+)?", RegexOption.IGNORE_CASE), "").trim()
            deleteOrCancelAppointmentFromQuery(q)
        } else if (lower.startsWith("fissa") || lower.contains("appuntamento") || lower.contains("riunione") || lower.contains("call") || lower.contains("meeting")) {
            createAppointmentFromInstruction(text)
        } else if (lower.contains("agenda") || lower.contains("cosa ho") || lower.contains("impegni") || lower == "oggi") {
            viewModelScope.launch {
                val evs = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
                val reply = if (evs.isEmpty()) {
                    "📅 **Agenda di Oggi:** Nessun impegno clinico in programma."
                } else {
                    buildString {
                        appendLine("📅 **Agenda Clinica di Oggi (${evs.size} impegni):**")
                        for (ev in evs) {
                            val startH = ev.startIso.drop(11).take(5)
                            val endH = ev.endIso.drop(11).take(5)
                            appendLine("• **$startH - $endH** | **${ev.title}** (${ev.location})")
                        }
                    }.trimEnd()
                }
                val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = reply)
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
            }
        } else if (lower.contains("email") || lower.contains("mail") || lower.contains("posta") || lower.contains("resoconto") || lower.contains("sintesi")) {
            viewModelScope.launch {
                val briefing = generateEmailExecutiveBriefing()
                val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = briefing)
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
            }
        } else if (lower.contains("progett") || lower.contains("ricerca") || lower.contains("dossier") || lower.contains("studi")) {
            setProjectsSheetVisible(true)
            viewModelScope.launch {
                val projects = projectRepository.getProjects()
                val reply = buildString {
                    appendLine("🔬 **Dossier Ricerca MeDiTech (${projects.size} attivi):**")
                    for (p in projects.take(3)) {
                        appendLine("• **${p.codice}** — **${p.titolo}** (${p.stato})")
                    }
                }.trimEnd()
                val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = reply)
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
            }
        } else {
            // Elaborazione clinica 100% On-Device con Qwen 3.5 2B 4-bit quantizzato
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isAiProcessing = true)
                val modelName = _uiState.value.selectedAiModel
                val history = _uiState.value.chatMessages

                val options = when (_uiState.value.responseStyle) {
                    "Veloce & Diretto" -> OnDeviceGenerationOptions.FAST_DIRECT
                    "Approfondito" -> OnDeviceGenerationOptions.DEEP_REASONING
                    else -> OnDeviceGenerationOptions.BALANCED_CLINICAL
                }

                val groundedPrompt = OnDeviceLocalAiClient.buildGroundedSystemPrompt(
                    currentDateStr = _uiState.value.selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.ITALIAN)),
                    todayEventsCount = _uiState.value.events.size,
                    activeProjectsCount = _uiState.value.projects.size
                )

                val localResult = onDeviceAiClient.generateChatResponse(
                    modelName = modelName,
                    messages = history,
                    systemPrompt = groundedPrompt,
                    options = options
                )

                val reply = localResult.getOrElse {
                    "Sono a disposizione per supportarti nella gestione dell'agenda clinica, nella consultazione dei progetti MeDiTech e nelle comunicazioni con il Prof. Stéphane Meystre.\n\n" +
                    "💡 *Digita /aiuto per consultare l'elenco dei comandi rapidi.*"
                }

                _uiState.value = _uiState.value.copy(isAiProcessing = false)
                val assistantMsg = ChatMessage(id = UUID.randomUUID().toString().take(8), role = "assistant", text = reply)
                _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + assistantMsg)
            }
        }
    }

    fun setVoiceListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isVoiceListening = listening)
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(statusBanner = null)
    }

    fun checkForAppUpdate(force: Boolean = false) {
        viewModelScope.launch {
            val result = otaUpdateManager.checkForUpdate(force = force)
            if (result.status == com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus.UPDATE_AVAILABLE) {
                val manifest = result.manifest
                val cachedApk = if (manifest != null) {
                    otaUpdateManager.getExistingDownloadedApk(
                        targetVersionCode = manifest.latestVersionCode,
                        expectedSha256 = manifest.sha256
                    )
                } else null

                val isNewer = manifest != null && manifest.latestVersionCode > result.currentVersionCode
                val bannerText = if (isNewer) {
                    "Nuovo aggiornamento v${manifest.latestVersionName} disponibile!"
                } else {
                    "Versione v${manifest?.latestVersionName ?: result.currentVersionName} pronta (Reinstallazione/Aggiornamento)"
                }

                _uiState.value = _uiState.value.copy(
                    isUpdateAvailable = true,
                    updateManifest = manifest,
                    readyToInstallApk = cachedApk,
                    statusBanner = bannerText
                )
            } else if (result.status == com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus.UP_TO_DATE) {
                _uiState.value = _uiState.value.copy(
                    statusBanner = "L'app è aggiornata all'ultima versione (v${result.currentVersionName})"
                )
            } else if (result.status == com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus.CHECK_FAILED) {
                _uiState.value = _uiState.value.copy(
                    statusBanner = "Verifica aggiornamenti: ${result.errorMessage ?: "Errore di rete"}"
                )
            }
        }
    }

    fun startOtaDownload(context: Context, onReadyToInstall: (java.io.File) -> Unit = {}) {
        val manifest = _uiState.value.updateManifest ?: return
        viewModelScope.launch {
            // Se il file APK è già scaricato e verificato nella cache, procedi subito all'installazione
            val existingApk = otaUpdateManager.getExistingDownloadedApk(
                targetVersionCode = manifest.latestVersionCode,
                expectedSha256 = manifest.sha256
            )
            if (existingApk != null && existingApk.exists()) {
                _uiState.value = _uiState.value.copy(
                    readyToInstallApk = existingApk,
                    isDownloadingUpdate = false,
                    updateProgress = 1f,
                    statusBanner = "Pacchetto v${manifest.latestVersionName} pronto per l'installazione!"
                )
                onReadyToInstall(existingApk)
                installOrPromptPermission(context, existingApk)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isDownloadingUpdate = true, updateProgress = 0f)
            val result = otaUpdateManager.downloadApk(manifest.downloadUrl, manifest.latestVersionCode, manifest.sha256) { progress, _, _ ->
                _uiState.value = _uiState.value.copy(updateProgress = progress)
            }
            _uiState.value = _uiState.value.copy(isDownloadingUpdate = false)
            result.onSuccess { apkFile ->
                _uiState.value = _uiState.value.copy(
                    readyToInstallApk = apkFile,
                    updateProgress = 1f,
                    statusBanner = "Download completato! Avvio installazione..."
                )
                onReadyToInstall(apkFile)
                installOrPromptPermission(context, apkFile)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    statusBanner = "Errore download aggiornamento: ${err.localizedMessage ?: "Errore di connessione"}"
                )
            }
        }
    }

    fun installOrPromptPermission(context: Context, apkFile: java.io.File) {
        if (otaUpdateManager.canRequestPackageInstalls()) {
            context.startActivity(otaUpdateManager.createInstallIntent(apkFile))
            _uiState.value = _uiState.value.copy(isUpdateAvailable = false)
        } else {
            _uiState.value = _uiState.value.copy(
                statusBanner = "Autorizza l'installazione di app sconosciute, poi tocca 'Installa Ora'."
            )
            context.startActivity(otaUpdateManager.createManageUnknownAppSourcesIntent())
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(isUpdateAvailable = false)
    }

    fun setAiModel(model: String) {
        _uiState.value = _uiState.value.copy(
            selectedAiModel = model,
            statusBanner = "Modello attivo: $model"
        )
    }

    fun setResponseStyle(style: String) {
        _uiState.value = _uiState.value.copy(
            responseStyle = style,
            statusBanner = "Stile risposta: $style"
        )
    }

    fun toggleAutoCreateMeet(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoCreateMeet = enabled)
    }

    fun toggleSafetyBuffer(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(safetyBufferEnabled = enabled)
    }

    fun updateOnDeviceModelPath(path: String) {
        _uiState.value = _uiState.value.copy(
            onDeviceModelPath = path.trim(),
            statusBanner = "Percorso modello locale aggiornato: ${path.trim()}"
        )
    }

    fun checkOnDeviceModelFile(path: String) {
        val status = onDeviceAiClient.checkOnDeviceModelFile(path)
        _uiState.value = _uiState.value.copy(
            onDeviceModelPath = path.trim(),
            onDeviceModelStatus = status.statusText,
            statusBanner = if (status.exists) "Qwen 3.5 2B (4-bit) verificato sullo smartphone" else "File pesi non trovato"
        )
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = visible)
    }
}
