package com.example.assistentemeditechmobile.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.assistentemeditechmobile.domain.AppointmentParseResult
import com.example.assistentemeditechmobile.domain.CalendarEventItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.UUID

class CalendarRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private var cachedEvents: List<CalendarEventItem>? = null
    private val eventsFile: File
        get() = File(context.filesDir, "meditech_calendar_events.json")

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun hasCalendarPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    /**
     * Checks conflicts across both local app storage and system calendar with a buffer.
     */
    suspend fun checkConflicts(
        proposed: AppointmentParseResult,
        bufferMinutes: Long = 10L
    ): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val startIso = proposed.data_inizio_iso ?: return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val startMillis = runCatching {
            LocalDateTime.parse(startIso.take(19)).atZone(zone).toInstant().toEpochMilli()
        }.getOrNull() ?: return@withContext emptyList()

        val duration = if (proposed.durata_minuti > 0) proposed.durata_minuti else 60
        val endMillis = startMillis + (duration * 60_000L)

        val windowMin = startMillis - bufferMinutes * 60_000L
        val windowMax = endMillis + bufferMinutes * 60_000L

        val allEvents = getEventsInRange(windowMin - 3_600_000L, windowMax + 3_600_000L)
        allEvents.filter { ev ->
            com.example.assistentemeditechmobile.domain.CalendarConflictEvaluator.hasConflict(
                existingStart = ev.startMillis,
                existingEnd = ev.endMillis,
                isCancelled = ev.status == "CANCELLED",
                proposedStart = startMillis,
                proposedEnd = endMillis,
                bufferMinutes = bufferMinutes
            )
        }
    }

    /**
     * Creates calendar event with variable duration and 3 reminders:
     * - 24 hours before (1440 min)
     * - 2 hours before (120 min)
     * - 1 hour before (60 min)
     */
    suspend fun createEvent(proposed: AppointmentParseResult): CalendarEventItem = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val startDt = proposed.data_inizio_iso?.let {
            runCatching { LocalDateTime.parse(it.take(19)) }.getOrNull()
        } ?: LocalDateTime.now().plusHours(1)

        val duration = if (proposed.durata_minuti > 0) proposed.durata_minuti else 60
        val endDt = proposed.data_fine_iso?.let {
            runCatching { LocalDateTime.parse(it.take(19)) }.getOrNull()
        } ?: startDt.plusMinutes(duration.toLong())

        val startMillis = startDt.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDt.atZone(zone).toInstant().toEpochMilli()

        val isOnlineMeeting = proposed.is_online || listOf("meet", "call", "online", "zoom", "teams", "remoto")
            .any { proposed.luogo.lowercase().contains(it) || proposed.title.lowercase().contains(it) }

        val hangoutLink = if (isOnlineMeeting) {
            "https://meet.google.com/meditech-${UUID.randomUUID().toString().take(6)}"
        } else ""

        var systemEventId = ""
        if (hasCalendarPermissions()) {
            systemEventId = insertIntoSystemCalendar(
                title = proposed.title,
                description = "MeDiTech SUPSI - Contatto: ${proposed.contatto}\nDurata: $duration min",
                location = if (isOnlineMeeting) "Google Meet / Online" else proposed.luogo,
                startMillis = startMillis,
                endMillis = endMillis,
                hangoutLink = hangoutLink
            )
        }

        val eventItem = CalendarEventItem(
            id = if (systemEventId.isNotBlank()) systemEventId else UUID.randomUUID().toString().take(8),
            title = proposed.title,
            startIso = startDt.format(isoFormatter),
            endIso = endDt.format(isoFormatter),
            durationMinutes = duration,
            location = if (isOnlineMeeting) "Google Meet / Online" else proposed.luogo,
            contact = proposed.contatto,
            isOnline = isOnlineMeeting,
            hangoutLink = hangoutLink,
            status = "CONFIRMED",
            startMillis = startMillis,
            endMillis = endMillis
        )

        mutex.withLock {
            val current = getEventsInternal().toMutableList()
            current.removeAll { it.id == eventItem.id }
            current.add(0, eventItem)
            cachedEvents = current
            eventsFile.writeText(json.encodeToString(current))
        }

        eventItem
    }

    /**
     * Instant 1-tap cancellation / undo for calendar events.
     */
    suspend fun cancelEvent(eventId: String): Boolean = withContext(Dispatchers.IO) {
        if (hasCalendarPermissions() && eventId.toLongOrNull() != null) {
            runCatching {
                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())
                context.contentResolver.delete(deleteUri, null, null)
            }
        }
        mutex.withLock {
            val current = getEventsInternal().toMutableList()
            val idx = current.indexOfFirst { it.id == eventId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(status = "CANCELLED")
                cachedEvents = current
                eventsFile.writeText(json.encodeToString(current))
                true
            } else false
        }
    }

    suspend fun getEventsForDate(date: LocalDate): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        getEventsInRange(startMillis, endMillis)
    }

    suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val localEvents = mutex.withLock {
            getEventsInternal().filter { it.status != "CANCELLED" && it.startMillis < endMillis && it.endMillis > startMillis }
        }

        if (!hasCalendarPermissions()) {
            return@withContext localEvents
        }

        val systemEvents = querySystemCalendar(startMillis, endMillis)
        val combined = (localEvents + systemEvents).distinctBy { it.title + "_" + (it.startMillis / 60000) }
        combined.sortedBy { it.startMillis }
    }

    private fun insertIntoSystemCalendar(
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        hangoutLink: String
    ): String {
        val calId = getDefaultCalendarId() ?: return ""
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, if (hangoutLink.isNotBlank()) "$description\nMeet: $hangoutLink" else description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val uri = runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }.getOrNull()
        val eventId = uri?.lastPathSegment ?: ""

        if (eventId.isNotBlank()) {
            val reminders = listOf(1440, 120, 60)
            for (m in reminders) {
                val rValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId.toLong())
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, m)
                }
                runCatching { context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, rValues) }
            }
        }
        return eventId
    }

    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val cursor = runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
        }.getOrNull() ?: return null

        cursor.use { c ->
            var fallbackId: Long? = null
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val isPrimary = c.getInt(1) == 1
                if (isPrimary) return id
                if (fallbackId == null) fallbackId = id
            }
            return fallbackId
        }
    }

    private fun querySystemCalendar(startMillis: Long, endMillis: Long): List<CalendarEventItem> {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )
        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?) OR " +
                "(${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTEND} <= ?)"
        val args = arrayOf(startMillis.toString(), endMillis.toString(), startMillis.toString(), endMillis.toString())

        val cursor = runCatching {
            context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null)
        }.getOrNull() ?: return emptyList()

        val results = mutableListOf<CalendarEventItem>()
        val zone = ZoneId.systemDefault()
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0).toString()
                val title = c.getString(1) ?: "(Impegno senza titolo)"
                val sMillis = c.getLong(2)
                val eMillis = c.getLong(3).takeIf { it > sMillis } ?: (sMillis + 3600000L)
                val loc = c.getString(4) ?: ""
                val desc = c.getString(5) ?: ""

                val startDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(sMillis), zone)
                val endDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(eMillis), zone)
                val duration = ((eMillis - sMillis) / 60000L).toInt().coerceAtLeast(15)

                results.add(
                    CalendarEventItem(
                        id = id,
                        title = title,
                        startIso = startDt.format(isoFormatter),
                        endIso = endDt.format(isoFormatter),
                        durationMinutes = duration,
                        location = loc,
                        contact = desc,
                        isOnline = loc.lowercase().contains("meet") || desc.lowercase().contains("meet"),
                        hangoutLink = if (desc.contains("meet.google.com")) "https://meet.google.com" else "",
                        status = "CONFIRMED",
                        startMillis = sMillis,
                        endMillis = eMillis
                    )
                )
            }
        }
        return results
    }

    private fun getEventsInternal(): List<CalendarEventItem> {
        cachedEvents?.let { return it }
        if (!eventsFile.exists()) {
            cachedEvents = emptyList()
            return emptyList()
        }
        val loaded = runCatching {
            json.decodeFromString<List<CalendarEventItem>>(eventsFile.readText())
        }.getOrDefault(emptyList())
        cachedEvents = loaded
        return loaded
    }
}
