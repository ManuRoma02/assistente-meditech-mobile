package com.example.assistentemeditechmobile.domain

import com.example.assistentemeditechmobile.data.CalendarRepository
import com.example.assistentemeditechmobile.data.VipRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class MeditechAiEngine(
    private val calendarRepository: CalendarRepository? = null,
    private val vipRepository: VipRepository? = null
) {

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * Parses free-form user speech or text to extract:
     * - Title
     * - Date & start time
     * - Real duration (15, 30, 45, 60 min)
     * - Location / Google Meet
     * - Contact (e.g. Prof. Stéphane Meystre)
     */
    suspend fun parseAppointmentInstruction(text: String): AppointmentParseResult {
        val clean = text.trim()
        val lower = clean.lowercase()

        // 1. Detect contact (checking dynamic VIP repository if available)
        val dynamicVips = runCatching { vipRepository?.getVips() }.getOrNull() ?: emptyList()
        val matchedVip = dynamicVips.firstOrNull { vip ->
            lower.contains(vip.name.lowercase()) || (vip.emailOrDomain.isNotBlank() && lower.contains(vip.emailOrDomain.substringBefore("@").lowercase()))
        }
        val contact = when {
            matchedVip != null -> "${matchedVip.name} (${matchedVip.roleTitle})"
            lower.contains("meystre") -> "Prof. Stéphane Meystre (SUPSI MeDiTech)"
            lower.contains("fontana") -> "Dr.ssa Elena Fontana (EOC)"
            lower.contains("rossi") -> "Mario Rossi"
            else -> {
                val conMatch = Regex("(?:con|assieme a|insieme a)\\s+([A-ZÀ-ÖØ-ßa-zà-öø-ÿ\\s.]+?)(?:\\s+(?:in|alle|su|per|il|domani)|$)").find(clean)
                conMatch?.groupValues?.get(1)?.trim() ?: "Collaboratore MeDiTech"
            }
        }

        // 2. Detect real duration (Problem #3 solved!)
        val durationMinutes = when {
            Regex("\\b(15|quindici)\\s*minut").containsMatchIn(lower) -> 15
            Regex("\\b(20|venti)\\s*minut").containsMatchIn(lower) -> 20
            Regex("\\b(30|trenta)\\s*minut|mezz'ora|mezzora").containsMatchIn(lower) -> 30
            Regex("\\b(45|quarantacinque)\\s*minut").containsMatchIn(lower) -> 45
            Regex("\\b(2|due)\\s*ore").containsMatchIn(lower) -> 120
            Regex("\\b(1|un'|una)\\s*ora").containsMatchIn(lower) -> 60
            else -> 30 // Default to smart 30 min for clinical/work meetings
        }

        // 3. Detect date
        val today = LocalDate.now()
        val targetDate = when {
            lower.contains("dopodomani") -> today.plusDays(2)
            lower.contains("domani") -> today.plusDays(1)
            lower.contains("lunedì") || lower.contains("lunedi") -> nextDayOfWeek(today, 1)
            lower.contains("martedì") || lower.contains("martedi") -> nextDayOfWeek(today, 2)
            lower.contains("mercoledì") || lower.contains("mercoledi") -> nextDayOfWeek(today, 3)
            lower.contains("giovedì") || lower.contains("giovedi") -> nextDayOfWeek(today, 4)
            lower.contains("venerdì") || lower.contains("venerdi") -> nextDayOfWeek(today, 5)
            else -> today
        }

        // 4. Detect time
        var hour = 10
        var minute = 0
        val timeMatch = Regex("(?:alle|ore)\\s*(\\d{1,2})(?:[:.](\\d{2}))?").find(lower)
        if (timeMatch != null) {
            hour = timeMatch.groupValues[1].toIntOrNull() ?: 10
            minute = timeMatch.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        }

        val startDt = targetDate.atTime(hour, minute, 0)
        val endDt = startDt.plusMinutes(durationMinutes.toLong())

        // 5. Detect online meeting (Google Meet)
        val isOnline = listOf("meet", "call", "online", "zoom", "teams", "remoto")
            .any { lower.contains(it) }

        val location = when {
            isOnline -> "Google Meet / Videochiamata Online"
            lower.contains("laboratorio") || lower.contains("lab") -> "Laboratorio MeDiTech SUPSI (Polo Viganello)"
            lower.contains("ospedale") || lower.contains("eoc") -> "Ospedale Regionale Lugano (EOC)"
            lower.contains("ufficio") -> "Ufficio SUPSI MeDiTech"
            else -> "SUPSI MeDiTech (Lugano)"
        }

        val title = when {
            clean.startsWith("fissa", ignoreCase = true) -> clean.replaceFirst(Regex("^fissami\\s+|^fissa\\s+", RegexOption.IGNORE_CASE), "").capitalizeWords()
            else -> "Meeting: $contact"
        }

        return AppointmentParseResult(
            title = title.ifBlank { "Allineamento MeDiTech con $contact" },
            data_inizio_iso = startDt.format(isoFormatter),
            data_fine_iso = endDt.format(isoFormatter),
            durata_minuti = durationMinutes,
            luogo = location,
            contatto = contact,
            is_online = isOnline,
            is_ambiguous = false,
            ambiguity_reason = ""
        )
    }

    private fun nextDayOfWeek(base: LocalDate, targetDayOfWeek: Int): LocalDate {
        var d = base.plusDays(1)
        while (d.dayOfWeek.value != targetDayOfWeek) {
            d = d.plusDays(1)
        }
        return d
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
