package com.example.assistentemeditechmobile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.CalendarEventItem
import com.example.assistentemeditechmobile.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schermata Agenda Clinica MeDiTech SUPSI:
 * - Selettore orizzontale a capsule Apple Calendar (14 giorni)
 * - Statistiche dinamiche della giornata (impegni totali, ore previste, conflitti)
 * - Card impegni ad altissimo contrasto con categorie cliniche MeDiTech
 * - Rilevamento visivo dei conflitti orari con alert dedicato
 * - 1-Tap Google Meet & 1-Tap Google Maps Sede
 */
@Composable
fun AgendaClinicaScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newEventInstruction by remember { mutableStateOf("") }
    var eventToCancel by remember { mutableStateOf<CalendarEventItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 14.dp)
    ) {
        // 1. Apple Calendar Horizontal Date Strip (14 giorni da ieri a +12)
        DateSelectorStrip(
            selectedDate = uiState.selectedDate,
            onDateSelected = { date ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectDate(date)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Daily Header Card con statistiche cliniche
        val isToday = uiState.selectedDate == LocalDate.now()
        val dayLabel = if (isToday) "OGGI" else uiState.selectedDate.format(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
        ).uppercase()
        val totalMinutes = uiState.events.sumOf { it.durationMinutes }
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val timeLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        val hasAnyConflict = uiState.events.any { it.hasConflict }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MeditechSurface,
            border = BorderStroke(0.8.dp, if (hasAnyConflict) MeditechWarningAmber.copy(alpha = 0.5f) else MeditechBorderCyan),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AGENDA CLINICA SUPSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            ),
                            color = MeditechCyanPrimary
                        )
                        if (isToday) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MeditechTealSoft
                            ) {
                                Text(
                                    text = "OGGI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MeditechTealActive,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MeditechTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "${uiState.events.size} appuntamenti • $timeLabel occupati" +
                                if (hasAnyConflict) " • ⚠️ Conflitto orario!" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (hasAnyConflict) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (hasAnyConflict) MeditechWarningAmber else MeditechTextTertiary
                    )
                }

                val addInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddDialog = true
                    },
                    interactionSource = addInteraction,
                    shape = CircleShape,
                    color = MeditechCyanPrimary,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .cartoonPressSquash(addInteraction, pressedScaleY = 0.90f, squashScaleX = 1.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nuovo Appuntamento",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Lista Appuntamenti o Stato Vuoto Illustrato
        if (uiState.events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MeditechSurface,
                    border = BorderStroke(0.8.dp, MeditechBorder),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MeditechCyanSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Nessun Impegno Clinico",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MeditechTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "L'agenda del polo SUPSI MeDiTech è libera per questa data. Puoi chiedere all'agente AI on-device di pianificare un nuovo incontro.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = MeditechTextTertiary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fissa con l'IA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    ClinicalEventCard(
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            placementSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ),
                        event = event,
                        isDeleting = event.id in uiState.deletingEventIds,
                        onCancelClick = { eventToCancel = event },
                        onMeetClick = {
                            if (event.hangoutLink.isNotBlank()) {
                                val meetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.hangoutLink))
                                meetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(meetIntent)
                            }
                        },
                        onShareClick = {
                            val timeStart = event.startIso.substringAfter('T').take(5).ifBlank { "14:00" }
                            val timeEnd = event.endIso.substringAfter('T').take(5).ifBlank { "15:00" }
                            val shareText = buildString {
                                append("🩺 Impegno MeDiTech SUPSI:\n")
                                append("📌 ${event.title}\n")
                                append("🗓 Orario: $timeStart - $timeEnd (${event.durationMinutes} min)\n")
                                if (event.location.isNotBlank()) append("📍 Sede: ${event.location}\n")
                                if (event.isOnline && event.hangoutLink.isNotBlank()) {
                                    append("💻 Google Meet: ${event.hangoutLink}\n")
                                }
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooser = Intent.createChooser(sendIntent, "Condividi Impegno Clinico:")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                    )
                }
            }
        }
    }

    // Dialog Conferma Cancellazione Appuntamento
    eventToCancel?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToCancel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MeditechUrgentRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Annullare Impegno?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Vuoi rimuovere '${event.title}' dall'agenda MeDiTech? L'azione libererà lo slot sul calendario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeditechTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = event.id
                        eventToCancel = null
                        viewModel.cancelAppointment(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechUrgentRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Annulla Appuntamento", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToCancel = null }) {
                    Text("Mantieni", color = MeditechTextSecondary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog Nuovo Appuntamento Veloce
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fissa Appuntamento Clinico",
                        color = MeditechTextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Scrivi l'istruzione in linguaggio naturale, ad esempio:\n• 'Riunione 45 min con Stéphane Meystre alle 14:30 in laboratorio'\n• 'Call telemetria P4 domani alle 11 su Meet'",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        color = MeditechTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newEventInstruction,
                        onValueChange = { newEventInstruction = it },
                        placeholder = { Text("es: Meeting con Stéphane Meystre alle 15", fontSize = 13.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEventInstruction.isNotBlank()) {
                            viewModel.createAppointmentFromInstruction(newEventInstruction)
                            newEventInstruction = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Elabora con IA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annulla", color = MeditechTextTertiary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Selettore a nastro di date orizzontale in stile Apple HIG Calendar.
 */
@Composable
private fun DateSelectorStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    // Mostra da ieri (-1) a +12 giorni
    val dates = remember { (-1..12).map { today.plusDays(it.toLong()) } }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isDateToday = date == today
            val dayName = when (date) {
                today -> "OGGI"
                today.plusDays(1) -> "DOM"
                else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.ITALIAN)).uppercase()
            }
            val dayNumber = date.dayOfMonth.toString()

            val pillBg by animateColorAsState(
                targetValue = if (isSelected) MeditechCyanPrimary else MeditechSurface,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "date_pill_bg"
            )
            val pillBorder by animateColorAsState(
                targetValue = if (isSelected) MeditechCyanPrimary else if (isDateToday) MeditechBorderCyan else MeditechBorder,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "date_pill_border"
            )
            val pillTextPrimary by animateColorAsState(
                targetValue = if (isSelected) Color.White else MeditechTextPrimary,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "date_pill_text_primary"
            )
            val pillTextSecondary by animateColorAsState(
                targetValue = if (isSelected) Color.White.copy(alpha = 0.85f) else if (isDateToday) MeditechCyanPrimary else MeditechTextTertiary,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "date_pill_text_secondary"
            )

            val dateInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = { onDateSelected(date) },
                interactionSource = dateInteraction,
                shape = RoundedCornerShape(16.dp),
                color = pillBg,
                border = BorderStroke(0.8.dp, pillBorder),
                shadowElevation = if (isSelected) 2.dp else 0.5.dp,
                modifier = Modifier
                    .width(62.dp)
                    .cartoonPressSquash(dateInteraction, pressedScaleY = 0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || isDateToday) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = pillTextSecondary
                    )
                    Text(
                        text = dayNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = pillTextPrimary
                    )
                    // Indicatore pallino per la giornata odierna
                    if (isDateToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else MeditechCyanPrimary)
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Card Impegno Clinico ad altissimo contrasto con categorie cliniche e opzioni rapide.
 */
@Composable
fun ClinicalEventCard(
    event: CalendarEventItem,
    isDeleting: Boolean = false,
    onCancelClick: () -> Unit,
    onMeetClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Categorizzazione semantica colore MeDiTech
    val (categoryTitle, categoryColor, categoryBg) = remember(event.title) {
        val lower = event.title.lowercase()
        when {
            lower.contains("telemetr") || lower.contains("sensori") || lower.contains("p4") ->
                Triple("RICERCA TELEMETRIA", MeditechTealActive, MeditechTealSoft)
            lower.contains("urgen") || lower.contains("triage") || lower.contains("visita") ->
                Triple("TRIAGE CLINICO", MeditechUrgentRed, MeditechUrgentSoft)
            lower.contains("supsi") || lower.contains("meystre") || lower.contains("polo") ->
                Triple("RICERCA ME-DITECH SUPSI", MeditechCyanDark, MeditechCyanSoft)
            else ->
                Triple("MEETING CLINICO", MeditechCyanPrimary, MeditechCyanSoft)
        }
    }

    val timeStart = event.startIso.substringAfter('T').take(5).ifBlank { "14:00" }
    val timeEnd = event.endIso.substringAfter('T').take(5).ifBlank { "15:00" }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeditechSurface,
        border = BorderStroke(
            0.8.dp,
            if (event.hasConflict) MeditechWarningAmber.copy(alpha = 0.6f) else MeditechBorder
        ),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .cartoonCardExit(isDeleting)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Riga laterale colorata della categoria
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.5.dp)
                    .background(categoryColor)
            )

            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                // Header: Categoria + Orario + Durata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryBg
                    ) {
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MeditechCyanSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MeditechCyanDark,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "$timeStart - $timeEnd",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MeditechCyanDark
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MeditechGroupedBg
                        ) {
                            Text(
                                text = "${event.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MeditechTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Titolo Appuntamento
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ),
                    color = MeditechTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Dettagli Sede e Contatto
                if (event.contact.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = event.contact,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MeditechTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.location.ifBlank { "SUPSI Viganello, Lab MeDiTech" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Banner Avviso Conflitto Orario (se rilevato dal motore)
                if (event.hasConflict) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MeditechWarningSoft,
                        border = BorderStroke(0.6.dp, MeditechWarningAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MeditechWarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = event.conflictDetails.ifBlank { "Sovrapposizione oraria rilevata con un altro impegno!" },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MeditechWarningAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Azioni Rapide: Google Meet + Mappe + Condividi + Annulla
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.isOnline && event.hangoutLink.isNotBlank()) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMeetClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Meet",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }

                    // Tasto Sede Mappe
                    val locInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val loc = event.location.ifBlank { "SUPSI Viganello Lugano" }
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(loc)))
                            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(mapIntent)
                        },
                        interactionSource = locInteraction,
                        shape = RoundedCornerShape(10.dp),
                        color = MeditechGroupedBg,
                        border = BorderStroke(0.6.dp, MeditechBorder),
                        modifier = Modifier
                            .weight(1f)
                            .cartoonPressSquash(locInteraction, pressedScaleY = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Mappe",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MeditechTextPrimary
                            )
                        }
                    }

                    // Tasto Condividi
                    val shareInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShareClick()
                        },
                        interactionSource = shareInteraction,
                        shape = RoundedCornerShape(10.dp),
                        color = MeditechCyanSoft,
                        border = BorderStroke(0.6.dp, MeditechBorderCyan),
                        modifier = Modifier
                            .weight(1f)
                            .cartoonPressSquash(shareInteraction, pressedScaleY = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MeditechCyanDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Invia",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MeditechCyanDark
                            )
                        }
                    }

                    // Tasto Annulla
                    val cancelInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancelClick()
                        },
                        interactionSource = cancelInteraction,
                        shape = RoundedCornerShape(10.dp),
                        color = MeditechUrgentSoft,
                        border = BorderStroke(0.6.dp, MeditechUrgentRed.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .cartoonPressSquash(cancelInteraction, pressedScaleY = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MeditechUrgentRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Annulla",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MeditechUrgentRed
                            )
                        }
                    }
                }
            }
        }
    }
}
