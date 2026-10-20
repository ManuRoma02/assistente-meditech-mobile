package com.example.assistentemeditechmobile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.CalendarEventItem
import com.example.assistentemeditechmobile.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AgendaClinicaScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 16.dp)
    ) {
        // Date Selector Bar (Apple HIG Segmented Style)
        DateSelectorRow(
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.selectDate(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Header Card
        val isToday = uiState.selectedDate == LocalDate.now()
        val dayLabel = if (isToday) "OGGI" else uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)).uppercase()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MeditechSurface),
            border = BorderStroke(1.dp, MeditechBorderCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AGENDA CLINICA & MEETING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MeditechCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = MeditechTextPrimary
                    )
                    Text(
                        text = "${uiState.events.size} impegni registrati",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextTertiary
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MeditechCyanPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuovo Appuntamento",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))        // Events List or Empty State
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
                    border = BorderStroke(1.dp, MeditechBorder),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanSoft,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MeditechCyanPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nessun Impegno Registrato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "La tua agenda MeDiTech è libera per questa giornata.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MeditechTextTertiary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aggiungi Appuntamento", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onCancelClick = { viewModel.cancelAppointment(event.id) },
                        onMeetClick = {
                            if (event.hangoutLink.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.hangoutLink))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Fissa Appuntamento Veloce", color = MeditechTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Scrivi l'istruzione in linguaggio naturale, es:\n'Allineamento 30 min con Stéphane Meystre alle 15 in laboratorio'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newEventTitle,
                        onValueChange = { newEventTitle = it },
                        label = { Text("Dettagli appuntamento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEventTitle.isNotBlank()) {
                            viewModel.createAppointmentFromInstruction(newEventTitle)
                            newEventTitle = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary)
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annulla", color = MeditechTextTertiary)
                }
            }
        )
    }
}

@Composable
fun EventCard(
    event: CalendarEventItem,
    onCancelClick: () -> Unit,
    onMeetClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MeditechSurface),
        border = BorderStroke(1.dp, MeditechBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MeditechCyanPrimary)
            )
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
            // Header Row: Time + Duration badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${event.startIso.takeLast(8).take(5)} - ${event.endIso.takeLast(8).take(5)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeditechCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MeditechCyanSoft
                    ) {
                        Text(
                            text = "${event.durationMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechCyanDark,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status pill
                Surface(
                    shape = CircleShape,
                    color = MeditechTealSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MeditechTealActive, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Confermato",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechTealActive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeditechTextPrimary
            )

            // Contact & Location
            if (event.contact.isNotBlank()) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MeditechTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.contact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeditechTextSecondary
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MeditechTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.location.ifBlank { "SUPSI MeDiTech" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MeditechTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row: Google Meet + Instant Undo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.isOnline) {
                    OutlinedButton(
                        onClick = onMeetClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MeditechCyanPrimary),
                        border = BorderStroke(1.dp, MeditechCyanPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Meet", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Problem #8 SOLVED: Instant Cancel Button
                FilledTonalButton(
                    onClick = onCancelClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MeditechUrgentSoft,
                        contentColor = MeditechUrgentRed
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = if (event.isOnline) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Annulla Appuntamento", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
}

@Composable
fun DateSelectorRow(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = (0..6).map { today.plusDays(it.toLong()) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val dayName = when (date) {
                today -> "Oggi"
                today.plusDays(1) -> "Domani"
                else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.ITALIAN)).replaceFirstChar { it.uppercase() }
            }
            val dayNumber = date.dayOfMonth.toString()

            Surface(
                onClick = { onDateSelected(date) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MeditechCyanPrimary else MeditechSurface,
                border = BorderStroke(1.dp, if (isSelected) MeditechCyanPrimary else MeditechBorder),
                modifier = Modifier.width(64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MeditechTextTertiary
                    )
                    Text(
                        text = dayNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MeditechTextPrimary
                    )
                }
            }
        }
    }
}
