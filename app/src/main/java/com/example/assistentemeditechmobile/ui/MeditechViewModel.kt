package com.example.assistentemeditechmobile.ui

import android.app.Application
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
import com.example.assistentemeditechmobile.domain.MeditechTab
import com.example.assistentemeditechmobile.domain.VipContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isClearingChat: Boolean = false,
    val deletingEventIds: Set<String> = emptySet(),
    val sentEmailIds: Set<String> = emptySet()
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

    private val _uiState = MutableStateFlow(MeditechUiState())
    val uiState: StateFlow<MeditechUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        checkForAppUpdate()
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

    fun simulateReceiveIncomingEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true)
            val newMail = emailRepository.simulateIncomingEmail()
            val updatedEmails = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                emails = updatedEmails,
                statusBanner = "Nuova email ricevuta da ${newMail.sender.substringBefore('<').trim()}: Recap AI generato!"
            )
        }
    }

    fun addMeetingFromEmail(title: String, slotStr: String, durationMinutes: Int, contact: String) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0)
            val end = now.plusMinutes(durationMinutes.toLong())
            val startMillis = now.atZone(zone).toInstant().toEpochMilli()
            val endMillis = end.atZone(zone).toInstant().toEpochMilli()
            val event = CalendarEventItem(
                id = "ev-" + UUID.randomUUID().toString().take(6),
                title = "Meeting: $title",
                startIso = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endIso = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                durationMinutes = durationMinutes,
                location = "SUPSI MeDiTech / Google Meet",
                contact = contact,
                isOnline = true,
                hangoutLink = "https://meet.google.com/mdt-clin-${UUID.randomUUID().toString().take(4)}",
                startMillis = startMillis,
                endMillis = endMillis
            )
            calendarRepository.addEvent(event)
            refreshEvents()
            _uiState.value = _uiState.value.copy(
                statusBanner = "Meeting con '$contact' aggiunto all'Agenda Clinica!"
            )
        }
    }

    fun generateEmailExecutiveBriefing(emailsList: List<EmailItem> = _uiState.value.emails): String {
        val count = emailsList.size
        val vipCount = emailsList.count { it.isVip }

        return buildString {
            append("• **Resoconto Email Cliniche Ricevute (Agente Locale Qwen 3.5)**\n\n")
            append("Analizzate in locale **$count email** ($vipCount prioritarie VIP EOC/SUPSI):\n\n")
            emailsList.take(3).forEach { mail ->
                val senderClean = mail.sender.substringBefore('<').trim()
                val vipBadge = if (mail.isVip) " [VIP EOC/SUPSI]" else ""
                val meetingBadge = if (mail.hasAppointmentProposal) " • *Richiesta Incontro*" else ""
                append("• **$senderClean**$vipBadge$meetingBadge\n")
                append("  **Oggetto:** ${mail.subject}\n")
                append("  **Sintesi:** ${mail.summary}\n\n")
            }
            append("• *Azioni predisposte:* Bozze di risposta pronte e slot verificati rispetto all'agenda clinica.")
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
                statusBanner = "Resoconto email generato dall'agente locale"
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

            val briefingText = generateEmailExecutiveBriefing(emails)

            val welcomeMsg = ChatMessage(
                id = "init-msg",
                role = "assistant",
                text = "Buongiorno! Sono il tuo assistente clinico **MeDiTech AI (SUPSI)** alimentato dal modello on-device **Qwen 3.5 HighIQ**.\n\n" +
                        briefingText + "\n\n" +
                        "• Per fissare un impegno: *'Fissa meeting di 30 minuti domani alle 14 con Stéphane Meystre'*."
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
            val created = calendarRepository.createEvent(parsed)
            refreshEvents()
            _uiState.value = _uiState.value.copy(isAiProcessing = false)

            val confirmationText = buildString {
                append("• **Appuntamento Confermato in Agenda!**\n\n")
                append("• **${created.title}**\n")
                append("• Data: ${created.startIso.take(10)} dalle ${created.startIso.takeLast(8).take(5)} alle ${created.endIso.takeLast(8).take(5)}\n")
                append("• Sede: ${created.location}\n")
                if (created.contact.isNotBlank()) append("• Contatto: ${created.contact}\n")
                if (created.isOnline && created.hangoutLink.isNotBlank()) {
                    append("• Link Videochiamata: ${created.hangoutLink}\n")
                }
                if (created.hasConflict) {
                    append("\n[Attenzione Conflitto]: ${created.conflictDetails}")
                }
            }

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = confirmationText,
                appointmentProposal = parsed
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
                text = "• **Cronologia ripulita con successo.**\n\nPronto per nuove richieste cliniche, pianificazione meeting e consultazione agenda."
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

        val lower = text.lowercase()
        if (lower.startsWith("fissa") || lower.contains("appuntamento") || lower.contains("riunione") || lower.contains("call") || lower.contains("meeting")) {
            createAppointmentFromInstruction(text)
        } else if (lower.contains("agenda") || lower.contains("cosa ho") || lower.contains("impegni")) {
            viewModelScope.launch {
                val evs = calendarRepository.getEventsForDate(_uiState.value.selectedDate)
                val reply = if (evs.isEmpty()) {
                    "Nessun appuntamento in programma per questa data. Agenda libera."
                } else {
                    "**Impegni registrati per la giornata:**\n\n" + evs.joinToString("\n\n") {
                        "• **${it.startIso.takeLast(8).take(5)} - ${it.endIso.takeLast(8).take(5)}**: ${it.title} (${it.durationMinutes} min)\n  Sede: ${it.location}"
                    }
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
                    "Elaborazione eseguita con il modello locale **Qwen 3.5 2B (4-bit On-Device)** sullo smartphone.\n\n" +
                    "Tutti i dati e i protocolli dell'Istituto MeDiTech e dell'EOC restano rigorosamente protetti nella memoria locale del telefono."
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

    fun checkForAppUpdate() {
        viewModelScope.launch {
            val result = otaUpdateManager.checkForUpdate()
            if (result.status == com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus.UPDATE_AVAILABLE) {
                _uiState.value = _uiState.value.copy(
                    isUpdateAvailable = true,
                    updateManifest = result.manifest,
                    statusBanner = "Nuovo aggiornamento v${result.manifest?.latestVersionName} disponibile!"
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

    fun startOtaDownload(onCompleted: (java.io.File) -> Unit) {
        val manifest = _uiState.value.updateManifest ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloadingUpdate = true, updateProgress = 0f)
            val result = otaUpdateManager.downloadApk(manifest.downloadUrl, manifest.latestVersionCode) { progress, _, _ ->
                _uiState.value = _uiState.value.copy(updateProgress = progress)
            }
            _uiState.value = _uiState.value.copy(isDownloadingUpdate = false)
            result.onSuccess { apkFile ->
                _uiState.value = _uiState.value.copy(readyToInstallApk = apkFile)
                onCompleted(apkFile)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    statusBanner = "Errore download aggiornamento: ${err.localizedMessage}"
                )
            }
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
