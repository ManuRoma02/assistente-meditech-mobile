package com.example.assistentemeditechmobile.data

import com.example.assistentemeditechmobile.domain.CalendarEventItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalendarEventRangeSemanticsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private fun createEvent(
        id: String,
        title: String,
        startLdt: LocalDateTime,
        endLdt: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault()
    ): CalendarEventItem {
        val startMillis = startLdt.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endLdt.atZone(zone).toInstant().toEpochMilli()
        return CalendarEventItem(
            id = id,
            title = title,
            startIso = startLdt.format(isoFormatter),
            endIso = endLdt.format(isoFormatter),
            startMillis = startMillis,
            endMillis = endMillis,
            location = "SUPSI MeDiTech, Lugano-Viganello",
            contact = "Dr. Stéphane Meystre"
        )
    }

    private fun writeLocalEvents(file: File, events: List<CalendarEventItem>) {
        file.writeText(json.encodeToString(events))
    }

    @Test
    fun `test event ended at 21 of previous day is excluded from target day`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test1.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        val evPrevious = createEvent(
            id = "prev_1",
            title = "Briefing MeDiTech Sera",
            startLdt = LocalDateTime.of(2026, 10, 25, 20, 0),
            endLdt = LocalDateTime.of(2026, 10, 25, 21, 0),
            zone = zone
        )
        val evTarget = createEvent(
            id = "target_1",
            title = "Comitato Scientifico SUPSI",
            startLdt = LocalDateTime.of(2026, 10, 26, 10, 0),
            endLdt = LocalDateTime.of(2026, 10, 26, 11, 30),
            zone = zone
        )

        writeLocalEvents(eventsFile, listOf(evPrevious, evTarget))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val eventsOnTarget = repo.getEventsForDate(targetDay)
        assertEquals("Deve comparire solo l'evento del target day", 1, eventsOnTarget.size)
        assertEquals("target_1", eventsOnTarget[0].id)
    }

    @Test
    fun `test event ending exactly at midnight is excluded from next day`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test2.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        val evMidnightEnd = createEvent(
            id = "midnight_end",
            title = "Monitoraggio Notturno EOC",
            startLdt = LocalDateTime.of(2026, 10, 25, 23, 0),
            endLdt = LocalDateTime.of(2026, 10, 26, 0, 0),
            zone = zone
        )

        writeLocalEvents(eventsFile, listOf(evMidnightEnd))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val eventsOnTarget = repo.getEventsForDate(targetDay)
        assertTrue("L'evento che finisce esattamente a mezzanotte (00:00) deve essere escluso dal giorno che inizia a mezzanotte", eventsOnTarget.isEmpty())
    }

    @Test
    fun `test cross midnight event is included in both days`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test3.json")
        val zone = ZoneId.systemDefault()
        val day1 = LocalDate.of(2026, 10, 25)
        val day2 = LocalDate.of(2026, 10, 26)

        val evCrossMidnight = createEvent(
            id = "cross_1",
            title = "Acquisizione Biosegnali Wearable 24h",
            startLdt = LocalDateTime.of(2026, 10, 25, 22, 0),
            endLdt = LocalDateTime.of(2026, 10, 26, 2, 0),
            zone = zone
        )

        writeLocalEvents(eventsFile, listOf(evCrossMidnight))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val eventsDay1 = repo.getEventsForDate(day1)
        val eventsDay2 = repo.getEventsForDate(day2)

        assertEquals("L'evento cross-midnight deve comparire nel giorno di inizio", 1, eventsDay1.size)
        assertEquals("cross_1", eventsDay1[0].id)

        assertEquals("L'evento cross-midnight deve comparire nel giorno successivo", 1, eventsDay2.size)
        assertEquals("cross_1", eventsDay2[0].id)
    }

    @Test
    fun `test normalizeCalendarEventTimes with regular timed event`() {
        val zone = ZoneId.of("Europe/Rome")
        val startMillis = LocalDateTime.of(2026, 10, 26, 14, 0).atZone(zone).toInstant().toEpochMilli()
        val endMillis = LocalDateTime.of(2026, 10, 26, 15, 30).atZone(zone).toInstant().toEpochMilli()

        val (normStart, normEnd) = CalendarRepository.normalizeCalendarEventTimes(
            rawStart = startMillis,
            rawEnd = endMillis,
            isAllDay = false,
            durationStr = null,
            zone = zone
        )

        assertEquals(startMillis, normStart)
        assertEquals(endMillis, normEnd)
    }

    @Test
    fun `test normalizeCalendarEventTimes with all day event in UTC`() {
        val zone = ZoneId.of("Europe/Rome")
        // Android CalendarContract stores ALL_DAY start in UTC midnight: 2026-10-26 00:00:00 UTC
        val utcStartMillis = LocalDateTime.of(2026, 10, 26, 0, 0).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val utcEndMillis = LocalDateTime.of(2026, 10, 27, 0, 0).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

        val (localStart, localEnd) = CalendarRepository.normalizeCalendarEventTimes(
            rawStart = utcStartMillis,
            rawEnd = utcEndMillis,
            isAllDay = true,
            durationStr = null,
            zone = zone
        )

        val expectedLocalStart = LocalDate.of(2026, 10, 26).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedLocalEnd = LocalDate.of(2026, 10, 27).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expectedLocalStart, localStart)
        assertEquals(expectedLocalEnd, localEnd)
    }

    @Test
    fun `test resolveWritableCalendarId selects targetId when provided`() {
        val candidates = sequenceOf(
            CalendarRepository.Companion.CalendarCandidate(id = 1L, isPrimary = true, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 2L, isPrimary = false, accessLevel = 700)
        )

        val resolved = CalendarRepository.resolveWritableCalendarId(candidates, targetId = 2L)
        assertEquals(2L, resolved)
    }

    @Test
    fun `test resolveWritableCalendarId falls back to primary when targetId is null`() {
        val candidates = sequenceOf(
            CalendarRepository.Companion.CalendarCandidate(id = 10L, isPrimary = false, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 20L, isPrimary = true, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 30L, isPrimary = false, accessLevel = 700)
        )

        val resolved = CalendarRepository.resolveWritableCalendarId(candidates, targetId = null)
        assertEquals(20L, resolved)
    }
}
