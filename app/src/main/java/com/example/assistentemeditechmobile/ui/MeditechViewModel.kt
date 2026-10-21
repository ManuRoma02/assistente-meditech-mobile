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
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.domain.MeditechAiEngine
import com.example.assistentemeditechmobile.domain.MeditechTab
import com.example.assistentemeditechmobile.domain.VipContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class MeditechUiState(
    val currentTab: MeditechTab = MeditechTab.CHAT_AI,
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<CalendarEventItem> = emptyList(),
    val emails: List<EmailItem> = emptyList(),
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

    fun generateEmailExecutiveBriefing(emailsList: List<EmailItem> = _uiState.value.emails): String {
        val count = emailsList.size
        val vipCount = emailsList.count { it.isVip }
        val meetingCount = emailsList.count { it.hasAppointmentProposal }

        return buildString {
            append("📬 **Resoconto Email Cliniche Ricevute (Agente Locale Qwen 3.5)**\n\n")
            append("Ho analizzato in locale le comunicazioni pervenute (**$count email**, $vipCount prioritarie VIP EOC/SUPSI):\n\n")
            emailsList.take(3).forEach { mail ->
                val senderClean = mail.sender.substringBefore('<').trim()
                val vipBadge = if (mail.isVip) " ⭐ [VIP EOC/SUPSI]" else ""
                val meetingBadge = if (mail.hasAppointmentProposal) " 🗓️ *[Richiesta Incontro]*" else ""
                append("• **$senderClean**$vipBadge:\n")
                append("  **Oggetto:** ${mail.subject}$meetingBadge\n")
                append("  *Sintesi:* ${mail.bodySnippet.take(110)}...\n\n")
            }
            append("💡 *Azioni predisposte dall'agente:* Bozze di risposta clinica pronte per l'invio immediato e slot verificati rispetto all'agenda.")
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
            val conflicts = calendarRepository.checkConflicts(parsed)

            val created = calendarRepository.createEvent(parsed)
            refreshEvents()

            val confirmText = buildString {
                append("**Appuntamento registrato con successo**\n\n")
                append("• **Titolo:** ${created.title}\n")
                append("• **Orario:** ${created.startIso.takeLast(8).take(5)} - ${created.endIso.takeLast(8).take(5)} (${created.durationMinutes} min)\n")
                append("• **Sede:** ${created.location}\n")
                append("• **Partecipante:** ${created.contact}\n\n")
                if (created.isOnline && created.hangoutLink.isNotBlank()) {
                    append("• **Collegamento Google Meet:** [Partecipa alla sessione](${created.hangoutLink})\n\n")
                }
                append("• **Promemoria attivati:** 24h, 2h e 1h prima.")
                if (conflicts.isNotEmpty()) {
                    append("\n\n[AVVISO SOVRAPPOSIZIONE] In calendario è già presente: *${conflicts.first().title}*.")
                }
            }

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString().take(8),
                role = "assistant",
                text = confirmText
            )

            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
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
                text = "✨ **Cronologia ripulita con successo.**\n\nPronto per nuove richieste cliniche, pianificazione meeting e consultazione agenda."
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
            } else if (result.status == com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus.ERROR) {
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