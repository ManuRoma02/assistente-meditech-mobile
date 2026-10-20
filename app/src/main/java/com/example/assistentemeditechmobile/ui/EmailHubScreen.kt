package com.example.assistentemeditechmobile.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.theme.*

@Composable
fun EmailHubScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedFilter by remember { mutableStateOf("Tutte") }
    var showVipDialog by remember { mutableStateOf(false) }

    // Dialog state for new VIP
    var newVipName by remember { mutableStateOf("") }
    var newVipEmail by remember { mutableStateOf("") }
    var newVipRole by remember { mutableStateOf("") }

    val filteredEmails = remember(uiState.emails, selectedFilter) {
        when (selectedFilter) {
            "VIP MeDiTech" -> uiState.emails.filter { it.isVip }
            "Con Meeting" -> uiState.emails.filter { it.hasAppointmentProposal }
            else -> uiState.emails
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header: Hub Title + VIP Manager Button (Problem #6 solved!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "EMAIL & INBOX ME-DI-TECH",
                    style = MaterialTheme.typography.labelMedium,
                    color = MeditechCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Caselle di Lavoro",
                    style = MaterialTheme.typography.titleLarge,
                    color = MeditechTextPrimary
                )
            }

            OutlinedButton(
                onClick = { showVipDialog = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MeditechCyanPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MeditechCyanPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("VIP Manager (${uiState.vips.size})", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Tutte", "VIP MeDiTech", "Con Meeting").forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeditechCyanPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MeditechSurface,
                        labelColor = MeditechTextSecondary
                    ),
                    border = BorderStroke(1.dp, if (isSelected) MeditechCyanPrimary else MeditechBorder)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Email Cards Feed
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredEmails, key = { it.id }) { email ->
                EmailCard(
                    email = email,
                    onGenerateDraft = { viewModel.generateReplyDraft(email) },
                    onSendReply = { draft -> viewModel.sendEmailReply(email, draft) },
                    onCopyDraft = { draft ->
                        clipboardManager.setText(AnnotatedString(draft))
                        Toast.makeText(context, "Bozza copiata negli appunti!", Toast.LENGTH_SHORT).show()
                    },
                    onScheduleMeeting = {
                        val prompt = "Fissa ${email.suggestedSlot} con ${email.sender.substringBefore('<')} ${email.suggestedDurationMinutes} minuti"
                        viewModel.createAppointmentFromInstruction(prompt)
                    }
                )
            }
        }
    }

    // VIP Management Dialog (Problem #6 SOLVED directly in mobile app!)
    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = { showVipDialog = false },
            title = { Text("Gestione Contatti VIP & MeDiTech", color = MeditechTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "I mittenti VIP ricevono notifiche ad alta priorità e non vengono mai filtrati dalle newsletter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newVipName,
                        onValueChange = { newVipName = it },
                        label = { Text("Nome (es. Prof. Stéphane Meystre)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newVipEmail,
                        onValueChange = { newVipEmail = it },
                        label = { Text("Email o Dominio (es. stephane.meystre@supsi.ch)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newVipRole,
                        onValueChange = { newVipRole = it },
                        label = { Text("Ruolo / Titolo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newVipName.isNotBlank() && newVipEmail.isNotBlank()) {
                                viewModel.addVipContact(newVipName, newVipEmail, newVipRole)
                                newVipName = ""
                                newVipEmail = ""
                                newVipRole = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aggiungi Contatto VIP")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("VIP Attualmente Registrati:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                        items(uiState.vips, key = { it.id }) { vip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vip.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MeditechTextPrimary)
                                    Text(vip.emailOrDomain, style = MaterialTheme.typography.labelSmall, color = MeditechTextTertiary)
                                }
                                IconButton(onClick = { viewModel.removeVipContact(vip.id) }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Rimuovi", tint = MeditechUrgentRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVipDialog = false }) {
                    Text("Chiudi", color = MeditechCyanPrimary)
                }
            }
        )
    }
}

@Composable
fun EmailCard(
    email: EmailItem,
    onGenerateDraft: () -> Unit,
    onSendReply: (String) -> Unit,
    onCopyDraft: (String) -> Unit,
    onScheduleMeeting: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MeditechSurface),
        border = BorderStroke(1.dp, if (email.isVip) MeditechCyanPrimary.copy(alpha = 0.6f) else MeditechBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Sender + VIP Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.sender.substringBefore('<').trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeditechTextPrimary
                    )

                    if (email.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MeditechCyanDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "VIP MeDiTech",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeditechCyanDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = email.dateStr.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeditechTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subject
            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeditechTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary Box (in original language)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MeditechCyanUltraLight,
                border = BorderStroke(1.dp, MeditechBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SINTESI ESECUTIVA (${email.language.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechCyanDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextPrimary
                    )
                }
            }

            // Meeting Proposal Banner if applicable
            if (email.hasAppointmentProposal) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MeditechTealSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MeditechTealActive, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Proposta: ${email.suggestedSlot} (${email.suggestedDurationMinutes}m)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MeditechTealActive
                            )
                        }

                        Button(
                            onClick = onScheduleMeeting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeditechTealActive),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Fissa Ora", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Draft Reply Box (Problem #2 SOLVED!)
            if (email.draftReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MeditechGroupedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "✍️ Bozza di Risposta Suggerita:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email.draftReply,
                            style = MaterialTheme.typography.bodySmall,
                            color = MeditechTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Direct Send (No copy-paste needed!)
                            Button(
                                onClick = { onSendReply(email.draftReply) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invia Subito", style = MaterialTheme.typography.labelSmall)
                            }

                            // Copy Button
                            OutlinedButton(
                                onClick = { onCopyDraft(email.draftReply) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MeditechBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copia", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGenerateDraft,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MeditechCyanPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MeditechCyanPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Genera Bozza Risposta con AI", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
