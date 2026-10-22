package com.example.assistentemeditechmobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistentemeditechmobile.data.CalendarRepository
import com.example.assistentemeditechmobile.data.ClinicalProjectRepository
import com.example.assistentemeditechmobile.data.EmailRepository
import com.example.assistentemeditechmobile.data.VipRepository
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
    val selectedAiModel: String = "Qwen 3.5 - 2B HighIQ (Locale On-Device)",
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
            val draft = emailRepository.generateAiDraft(emailItem, userInstruction)
            emailRepository.updateDraftReply(emailItem.id, draft)
            val updated = emailRepository.getEmails()
            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                emails = updated,
                statusBanner = "Bozza clinica predisposta per ${emailItem.sender.substringBefore('<').trim()}"
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
            // General clinical & MeDiTech response
            viewModelScope.launch {
                val reply = "Ho analizzato la tua richiesta in merito all'ambito clinico con il modello on-device **${_uiState.value.selectedAiModel}**.\n\n" +
                        "I protocolli attivi con l'EOC e il team del Prof. Stéphane Meystre sono costantemente monitorati in locale senza invio dati al cloud. Posso pianificare riunioni, redigere bozze per le email o riassumere i tuoi impegni."
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

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = visible)
    }
}
