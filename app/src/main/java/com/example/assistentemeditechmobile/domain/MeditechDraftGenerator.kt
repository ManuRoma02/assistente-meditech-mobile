package com.example.assistentemeditechmobile.domain

object MeditechDraftGenerator {

    fun generate(emailItem: EmailItem, userInstruction: String = ""): String {
        val isEnglish = emailItem.language.equals("en", ignoreCase = true) ||
                Regex("\\b(the|meeting|please|regards|thank|dear|project|hospital|trial|paper|conference)\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn("${emailItem.bodySnippet} ${emailItem.subject}")

        val senderName = emailItem.sender.substringBefore("<").trim().ifBlank {
            if (isEnglish) "Colleague" else "Gentile Collega"
        }
        val isMeystre = emailItem.sender.lowercase().contains("meystre")

        return if (isEnglish) {
            val greeting = if (isMeystre) "Gentile Prof. Meystre," else "Dear $senderName,"
            val signOff = "\n\nBest regards,\nAssistente - SUPSI MeDiTech Institute"
            val body = when {
                userInstruction.isNotBlank() -> {
                    "regarding your request: $userInstruction.\nPlease let me know if you need any further details."
                }
                emailItem.hasAppointmentProposal -> {
                    "I confirm my availability for the proposed meeting (${emailItem.suggestedSlot}). I have marked the appointment in my clinical calendar."
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
                emailItem.hasAppointmentProposal -> {
                    "ti confermo la disponibilità per l'incontro proposto (${emailItem.suggestedSlot}). Ho inserito l'appuntamento in calendario con relativo promemoria."
                }
                else -> {
                    "grazie per il messaggio e per gli aggiornamenti condivisi. Ho preso nota di quanto comunicato e ti confermo che stiamo procedendo secondo il piano prestabilito."
                }
            }
            "$greeting\n\n$body$signOff"
        }
    }
}
