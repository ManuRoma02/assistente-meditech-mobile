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
            recipient = "utente@supsi.ch",
            subject = "Allineamento Ricerca",
            dateStr = "20/10/2026 10:00",
            bodySnippet = "Ci vediamo domani alle 14:00?",
            summary = "Proposta incontro domani alle 14:00",
            hasAppointmentProposal = true,
            suggestedSlot = "Domani alle 14:00",
            language = "it"
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
            recipient = "utente@supsi.ch",
            subject = "Dataset EOC",
            dateStr = "20/10/2026 11:00",
            bodySnippet = "Abbiamo caricato i dati sul server.",
            summary = "Dati caricati",
            hasAppointmentProposal = false,
            language = "it"
        )

        val customInstruction = "ho già avviato la pipeline di anonimizzazione e ti invio il report entro stasera"
        val draft = MeditechDraftGenerator.generate(email, customInstruction)

        assertTrue(draft.contains("Gentile Dr.ssa Elena Fontana,"))
        assertTrue(draft.contains(customInstruction))
        assertTrue(draft.contains("Assistente MeDiTech - SUPSI Istituto MeDiTech"))
    }

    @Test
    fun testGenerateDraftForGeneralUpdate() {
        val email = EmailItem(
            id = "test-3",
            accountName = "SUPSI MeDiTech",
            sender = "Marco Bianchi <m.bianchi@partner.ch>",
            recipient = "utente@supsi.ch",
            subject = "Aggiornamento settimanale",
            dateStr = "20/10/2026 12:00",
            bodySnippet = "Tutto procede come previsto.",
            summary = "Aggiornamento positivo",
            hasAppointmentProposal = false,
            language = "it"
        )

        val draft = MeditechDraftGenerator.generate(email)

        assertTrue(draft.contains("Gentile Marco Bianchi,"))
        assertTrue(draft.contains("grazie per il messaggio e per gli aggiornamenti condivisi"))
    }

    @Test
    fun testGenerateDraftForEnglishEmailWithAppointmentProposal() {
        val email = EmailItem(
            id = "test-en-1",
            accountName = "International Collaborations",
            sender = "Dr. John Davis <j.davis@stanford-health.org>",
            recipient = "utente@supsi.ch",
            subject = "Clinical NLP Model Validation Meeting",
            dateStr = "22/10/2026 09:00",
            bodySnippet = "Dear colleague, could we schedule a quick call tomorrow at 3 PM to review the clinical NLP benchmarks?",
            summary = "Meeting proposal from Dr. Davis for tomorrow at 3 PM.",
            hasAppointmentProposal = true,
            suggestedSlot = "Tomorrow at 3 PM",
            language = "en"
        )

        val draft = MeditechDraftGenerator.generate(email)

        assertTrue(draft.contains("Dear Dr. John Davis,"))
        assertTrue(draft.contains("I confirm my availability for the proposed meeting"))
        assertTrue(draft.contains("Tomorrow at 3 PM"))
        assertTrue(draft.contains("Best regards,\nAssistente - SUPSI MeDiTech Institute"))
    }

    @Test
    fun testGenerateDraftForEnglishEmailWithCustomInstruction() {
        val email = EmailItem(
            id = "test-en-2",
            accountName = "International Collaborations",
            sender = "Prof. Alan Smith <a.smith@cambridge.ac.uk>",
            recipient = "utente@supsi.ch",
            subject = "Paper Submission Inquiry",
            dateStr = "22/10/2026 10:30",
            bodySnippet = "Please provide the latest figures for section 4.",
            summary = "Request for figures for section 4.",
            hasAppointmentProposal = false,
            language = "en"
        )

        val draft = MeditechDraftGenerator.generate(email, "I will send the high-res graphs by this evening")

        assertTrue(draft.contains("Dear Prof. Alan Smith,"))
        assertTrue(draft.contains("regarding your request: I will send the high-res graphs by this evening"))
        assertTrue(draft.contains("Best regards,\nAssistente - SUPSI MeDiTech Institute"))
    }
}
