package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.domain.MeditechDraftGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDraftTest {

    @Test
    fun testGenerateDraftForAppointmentProposal() {
        val email = EmailItem(
            id = "test-1",
            accountName = "SUPSI MeDiTech",
            sender = "Prof. Stéphane Meystre <stephane.meystre@supsi.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Allineamento Ricerca",
            dateStr = "20/10/2026 10:00",
            bodySnippet = "Ci vediamo domani alle 14:00?",
            summary = "Proposta incontro domani alle 14:00",
            hasAppointmentProposal = true,
            suggestedSlot = "Domani alle 14:00"
        )

        val draft = MeditechDraftGenerator.generate(email)

        assertTrue("Greeting should address Prof. Meystre with proper title", draft.contains("Gentile Prof. Meystre,"))
        assertTrue("Draft should reference the agreed slot", draft.contains("Domani alle 14:00"))
        assertTrue("Sign-off should include SUPSI Istituto MeDiTech", draft.contains("SUPSI Istituto MeDiTech"))
    }

    @Test
    fun testGenerateDraftWithCustomUserInstruction() {
        val email = EmailItem(
            id = "test-2",
            accountName = "SUPSI MeDiTech",
            sender = "Dr.ssa Elena Fontana <elena.fontana@eoc.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Dataset EOC",
            dateStr = "20/10/2026 11:00",
            bodySnippet = "Abbiamo caricato i dati sul server.",
            summary = "Dati caricati",
            hasAppointmentProposal = false
        )

        val customInstruction = "ho già avviato la pipeline di anonimizzazione e ti invio il report entro stasera"
        val draft = MeditechDraftGenerator.generate(email, customInstruction)

        assertTrue(draft.contains("Gentile Dr.ssa Elena Fontana,"))
        assertTrue(draft.contains(customInstruction))
        assertTrue(draft.contains("Manuele - SUPSI Istituto MeDiTech"))
    }

    @Test
    fun testGenerateDraftForGeneralUpdate() {
        val email = EmailItem(
            id = "test-3",
            accountName = "SUPSI MeDiTech",
            sender = "Marco Bianchi <m.bianchi@partner.ch>",
            recipient = "manuele@supsi.ch",
            subject = "Aggiornamento settimanale",
            dateStr = "20/10/2026 12:00",
            bodySnippet = "Tutto procede come previsto.",
            summary = "Aggiornamento positivo",
            hasAppointmentProposal = false
        )

        val draft = MeditechDraftGenerator.generate(email)

        assertTrue(draft.contains("Gentile Marco Bianchi,"))
        assertTrue(draft.contains("grazie per il messaggio e per gli aggiornamenti condivisi"))
    }
}
