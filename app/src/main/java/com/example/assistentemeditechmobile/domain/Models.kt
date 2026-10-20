package com.example.assistentemeditechmobile.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// =========================================================================
// Domain Models for SUPSI MeDiTech Mobile Assistant
// Clinical & Research Work Automation
// =========================================================================

enum class MeditechTab(val title: String) {
    AGENDA("Agenda Clinica"),
    EMAIL_HUB("Email & Inbox"),
    PROGETTI("Progetti & Studi"),
    CHAT_AI("Assistente MeDiTech")
}

@Immutable
@Serializable
data class EmailItem(
    val id: String,
    val accountName: String,
    val sender: String,
    val recipient: String,
    val subject: String,
    val dateStr: String,
    val bodySnippet: String,
    val summary: String,
    val language: String = "it",
    val isImportant: Boolean = false,
    val isVip: Boolean = false,
    val hasAppointmentProposal: Boolean = false,
    val suggestedSlot: String = "",
    val suggestedDurationMinutes: Int = 30,
    val draftReply: String = "",
    val attachments: List<String> = emptyList(),
    val isRead: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class CalendarEventItem(
    val id: String,
    val title: String,
    val startIso: String,
    val endIso: String,
    val durationMinutes: Int = 60,
    val location: String = "",
    val contact: String = "",
    val isOnline: Boolean = false,
    val hangoutLink: String = "",
    val hasConflict: Boolean = false,
    val conflictDetails: String = "",
    val status: String = "CONFIRMED",
    val remindersMinutes: List<Int> = listOf(1440, 120, 60),
    val startMillis: Long = 0L,
    val endMillis: Long = 0L
)

@Immutable
@Serializable
data class AppointmentParseResult(
    val title: String = "Riunione MeDiTech",
    val data_inizio_iso: String? = null,
    val data_fine_iso: String? = null,
    val durata_minuti: Int = 60,
    val luogo: String = "SUPSI MeDiTech / Online",
    val contatto: String = "",
    val is_online: Boolean = false,
    val is_ambiguous: Boolean = false,
    val ambiguity_reason: String = ""
)

@Immutable
@Serializable
data class ClinicalProject(
    val id: String,
    val codice: String,
    val titolo: String,
    val istituto: String = "SUPSI Istituto MeDiTech",
    val responsabileScientifico: String = "Prof. Stéphane Meystre",
    val stato: String = "Attivo",
    val descrizione: String = "",
    val documentCount: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class VipContact(
    val id: String,
    val name: String,
    val emailOrDomain: String,
    val roleTitle: String = "Collaboratore VIP",
    val isAutoHighPriority: Boolean = true,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isReasoning: Boolean = false,
    val appointmentProposal: AppointmentParseResult? = null,
    val emailDraftProposal: String? = null
)
