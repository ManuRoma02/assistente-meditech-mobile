package com.example.assistentemeditechmobile.domain

object MeditechDraftGenerator {

    fun generate(
        sender: String,
        subject: String,
        bodySnippet: String,
        userInstruction: String = "",
        isEnglishEmail: Boolean = false,
        hasAppointmentProposal: Boolean = false,
        suggestedSlot: String = ""
    ): String {
        val isEnglish = isEnglishEmail ||
                Regex("\\b(the|meeting|please|regards|thank|dear|project|hospital|trial|paper|conference)\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn("$bodySnippet $subject")

        val senderName = sender.substringBefore("<").trim().ifBlank {
            if (isEnglish) "Colleague" else "Gentile Collega"
        }
        val isMeystre = sender.lowercase().contains("meystre")

        return if (isEnglish) {
            val greeting = if (isMeystre) "Dear Prof. Meystre," else "Dear $senderName,"
            val signOff = "\n\nBest regards,\nAssistente - SUPSI MeDiTech Institute"
            val body = when {
                userInstruction.isNotBlank() -> {
                    "regarding your request: $userInstruction.\nPlease let me know if you need any further details."
                }
                hasAppointmentProposal -> {
                    val slotInfo = if (suggestedSlot.isNotBlank()) " ($suggestedSlot)" else ""
                    "I confirm my availability for the proposed meeting$slotInfo. I have marked the appointment in my clinical calendar."
                }
                bodySnippet.contains("proposal", ignoreCase = true) || subject.contains("meeting", ignoreCase = true) -> {
                    "I confirm my availability for the proposed meeting. I have marked the appointment in my clinical calendar."
                }
                else -> {
                    "thank you for your message and the updates provided. Everything is noted, and we are proceeding according to our research plan."
                }
            }
            "$greeting\n\n$body$signOff"
        } else {
            val greeting = if (isMeystre) "Gentile Prof. Meystre," else "Gentile $senderName,"
            val signOff = "\n\nCordiali saluti,\nAssistente MeDiTech - SUPSI Istituto MeDiTech"
            val body = when {
                userInstruction.isNotBlank() -> {
                    "in merito alla tua richiesta, $userInstruction.\nResto a completa disposizione per qualsiasi approfondimento."
                }
                hasAppointmentProposal -> {
                    val slotInfo = if (suggestedSlot.isNotBlank()) " ($suggestedSlot)" else ""
                    "ti confermo la disponibilità per l'incontro proposto$slotInfo. Ho inserito l'appuntamento in calendario con relativo promemoria."
                }
                bodySnippet.contains("incontro", ignoreCase = true) || subject.contains("appuntamento", ignoreCase = true) || subject.contains("riunione", ignoreCase = true) -> {
                    "ti confermo la disponibilità per l'incontro proposto. Ho inserito l'appuntamento in calendario con relativo promemoria."
                }
                else -> {
                    "grazie per il messaggio e per gli aggiornamenti condivisi. Ho preso nota di quanto comunicato e ti confermo che stiamo procedendo secondo il piano prestabilito."
                }
            }
            "$greeting\n\n$body$signOff"
        }
    }

    fun generate(emailItem: EmailItem, userInstruction: String = ""): String {
        return generate(
            sender = emailItem.sender,
            subject = emailItem.subject,
            bodySnippet = emailItem.bodySnippet,
            userInstruction = userInstruction,
            isEnglishEmail = emailItem.language.equals("en", ignoreCase = true),
            hasAppointmentProposal = emailItem.hasAppointmentProposal,
            suggestedSlot = emailItem.suggestedSlot
        )
    }
}
