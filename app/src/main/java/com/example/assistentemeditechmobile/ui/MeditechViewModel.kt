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
    val currentTab: MeditechTab = MeditechTab.AGENDA,
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
    val readyToInstallApk: java.io.File? = null
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

    fun loadInitialData() {
        viewModelScope.launch {
            val vips = vipRepository.getVips()
            val emails = emailRepository.getEmails()
            val projects = projectRepository.getProjects()
            val events = calendarRepository.getEventsForDate(_uiState.value.selectedDate)

            val welcomeMsg = ChatMessage(
                id = "init-msg",
                role = "assistant",
                text = "Buongiorno! Sono il tuo assistente clinico **MeDiTech AI (SUPSI)**.\n\n" +
                        "Ho sincronizzato le tue email di lavoro e la tua agenda. Come posso esserti utile?\n" +
                        "• Tocca il microfono o scrivi: *'Fissa meeting di 30 minuti domani alle 14 con il Prof. Stéphane Meystre in laboratorio'*."
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

    fun cancelAppointment(eventId: String) {
        viewModelScope.launch {
            val ok = calendarRepository.cancelEvent(eventId)
            if (ok) {
                refreshEvents()
                _uiState.value = _uiState.value.copy(statusBanner = "Appuntamento annullato con successo")
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
        } else {
            // General clinical & MeDiTech response
            viewModelScope.launch {
                val reply = "Ho analizzato la tua richiesta in merito all'ambito clinico e ai progetti MeDiTech. I protocolli attivi con l'EOC e il team del Prof. Stéphane Meystre sono costantemente monitorati. Posso pianificare riunioni, redigere bozze per le email o consultare i dossier."
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
                    updateManifest = result.manifest
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
}
