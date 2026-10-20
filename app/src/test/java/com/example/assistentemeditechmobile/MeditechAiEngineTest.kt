package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.domain.MeditechAiEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MeditechAiEngineTest {

    private lateinit var aiEngine: MeditechAiEngine

    @Before
    fun setUp() {
        aiEngine = MeditechAiEngine()
    }

    @Test
    fun testParse15MinuteQuickCall() = runTest {
        val input = "fissami una call rapida di 15 minuti con Stéphane Meystre domani alle 15:30"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals("Prof. Stéphane Meystre (SUPSI MeDiTech)", result.contatto)
        assertEquals(15, result.durata_minuti)
        assertTrue(result.is_online)
        assertTrue(result.luogo.contains("Google Meet"))
        assertTrue(result.data_inizio_iso!!.contains("T15:30:00"))
        assertTrue(result.data_fine_iso!!.contains("T15:45:00"))
    }

    @Test
    fun testParse45MinuteClinicalMeeting() = runTest {
        val input = "fissa allineamento di 45 minuti con Elena Fontana dopodomani alle 11:00"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals("Dr.ssa Elena Fontana (EOC)", result.contatto)
        assertEquals(45, result.durata_minuti)
        assertFalse(result.is_online)
        assertTrue(result.data_inizio_iso!!.contains("T11:00:00"))
        assertTrue(result.data_fine_iso!!.contains("T11:45:00"))
    }

    @Test
    fun testParseTwoHoursLabSession() = runTest {
        val input = "fissami laboratorio di 2 ore domani alle 09:30"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals(120, result.durata_minuti)
        assertTrue(result.luogo.contains("Laboratorio MeDiTech SUPSI"))
        assertTrue(result.data_inizio_iso!!.contains("T09:30:00"))
        assertTrue(result.data_fine_iso!!.contains("T11:30:00"))
    }

    @Test
    fun testParseDefault30MinuteDuration() = runTest {
        // Without explicit duration, clinical default is 30 minutes
        val input = "fissa un incontro con Mario Rossi domani alle 14:00"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals("Mario Rossi", result.contatto)
        assertEquals(30, result.durata_minuti)
        assertTrue(result.data_inizio_iso!!.contains("T14:00:00"))
        assertTrue(result.data_fine_iso!!.contains("T14:30:00"))
    }

    @Test
    fun testParseOneHourOnlineMeeting() = runTest {
        val input = "fissa un'ora di call su Teams con il responsabile clinico domani alle 16"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals(60, result.durata_minuti)
        assertTrue(result.is_online)
        assertTrue(result.data_inizio_iso!!.contains("T16:00:00"))
        assertTrue(result.data_fine_iso!!.contains("T17:00:00"))
    }

    @Test
    fun testExtractsCustomContactName() = runTest {
        val input = "fissa meeting con Marco Bernasconi domani alle 10:00"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertEquals("Marco Bernasconi", result.contatto)
    }

    @Test
    fun testCleansFissaPrefixFromTitle() = runTest {
        val input = "fissami revisione cartella clinica con Meystre domani alle 14"
        val result = aiEngine.parseAppointmentInstruction(input)

        assertTrue(result.title.startsWith("Revisione Cartella Clinica"))
        assertFalse(result.title.startsWith("fissami", ignoreCase = true))
    }
}
