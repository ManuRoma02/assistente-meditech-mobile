package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.ui.MessageBlock
import com.example.assistentemeditechmobile.ui.formatInlineMarkdown
import com.example.assistentemeditechmobile.ui.parseMessageBlocks
import org.junit.Assert.*
import org.junit.Test

class MeditechMessageRendererTest {

    @Test
    fun parseMessageBlocks_extractsHeadersCorrectly() {
        val markdown = """
            # Titolo Principale
            ## Sottotitolo
            ### Sezione Clinica
            Testo descrittivo del progetto.
        """.trimIndent()

        val blocks = parseMessageBlocks(markdown)
        assertTrue(blocks.any { it is MessageBlock.Header && it.level == 1 && it.text == "Titolo Principale" })
        assertTrue(blocks.any { it is MessageBlock.Header && it.level == 2 && it.text == "Sottotitolo" })
        assertTrue(blocks.any { it is MessageBlock.Header && it.level == 3 && it.text == "Sezione Clinica" })
        assertTrue(blocks.any { it is MessageBlock.Paragraph && it.text.contains("Testo descrittivo") })
    }

    @Test
    fun parseMessageBlocks_extractsBulletsAndNumberedItems() {
        val markdown = """
            Ecco i dettagli dell'incontro:
            • **Titolo**: Allineamento Ricerca
            • **Data**: 27 Ottobre 2026
            1. Primo passaggio
            2. Secondo passaggio
        """.trimIndent()

        val blocks = parseMessageBlocks(markdown)
        val bullets = blocks.filterIsInstance<MessageBlock.BulletItem>()
        assertEquals(2, bullets.size)
        assertTrue(bullets[0].text.contains("Allineamento Ricerca"))

        val numbered = blocks.filterIsInstance<MessageBlock.NumberedItem>()
        assertEquals(2, numbered.size)
        assertEquals("1", numbered[0].number)
        assertEquals("2", numbered[1].number)
    }

    @Test
    fun parseMessageBlocks_extractsCalloutAndDivider() {
        val markdown = """
            ---
            > Nota importante: monitorare i sensori
            [Attenzione Conflitto]: Incontro sovrapposto alle 15:00
        """.trimIndent()

        val blocks = parseMessageBlocks(markdown)
        assertTrue(blocks.any { it is MessageBlock.Divider })
        val callouts = blocks.filterIsInstance<MessageBlock.Callout>()
        assertEquals(2, callouts.size)
        assertFalse(callouts[0].isWarning)
        assertTrue(callouts[1].isWarning)
    }

    @Test
    fun formatInlineMarkdown_stripsRawAsterisksAndFormatsSpans() {
        val raw = "Meeting con **Stéphane Meystre** e codice `MDT-2026` confermato."
        val annotated = formatInlineMarkdown(raw, isUser = false)

        assertFalse("Raw asterisks should not be present in final text", annotated.text.contains("**"))
        assertEquals("Meeting con Stéphane Meystre e codice  MDT-2026  confermato.", annotated.text)
        assertTrue("AnnotatedString should contain bold and monospace spans", annotated.spanStyles.isNotEmpty())
    }
}
