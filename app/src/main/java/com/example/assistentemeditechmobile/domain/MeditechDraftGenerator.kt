package com.example.assistentemeditechmobile.domain

object MeditechDraftGenerator {

    fun generate(emailItem: EmailItem, userInstruction: String = ""): String {
        val senderName = emailItem.sender.substringBefore("<").trim().ifBlank { "Gentile Collega" }
        val isMeystre = emailItem.sender.lowercase().contains("meystre")

        val greeting = if (isMeystre) "Gentile Prof. Meystre," else "Gentile $senderName,"
        val signOff = "\n\nCordiali saluti,\nManuele - SUPSI Istituto MeDiTech"

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

        return "$greeting\n\n$body$signOff"
    }
}
