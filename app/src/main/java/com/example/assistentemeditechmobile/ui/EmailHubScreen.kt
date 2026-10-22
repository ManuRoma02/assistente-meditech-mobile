package com.example.assistentemeditechmobile.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailHubScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("Tutte") }
    var showVipDialog by remember { mutableStateOf(false) }

    // Dialog state for new VIP
    var newVipName by remember { mutableStateOf("") }
    var newVipEmail by remember { mutableStateOf("") }
    var newVipRole by remember { mutableStateOf("") }

    val filteredEmails = remember(uiState.emails, selectedFilter) {
        when (selectedFilter) {
            "VIP SUPSI" -> uiState.emails.filter { it.isVip }
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
        // Top Header: Title + Actions (Account Config, Simulate Receive, VIP Manager)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EMAIL CLINICHE & RESEARCH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MeditechCyanPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Caselle di Lavoro",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MeditechTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Simula Arrivo Nuova Email (con animazione e notifica)
                val syncInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { viewModel.simulateReceiveIncomingEmail() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MeditechCyanSoft, CircleShape)
                        .emilPressScale(syncInteraction)
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = "Simula Nuova Email",
                        tint = MeditechCyanDark,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Configurazione Account Email (Apple Style)
                val configInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { viewModel.setEmailConfigSheetVisible(true) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MeditechCyanSoft, CircleShape)
                        .emilPressScale(configInteraction)
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = "Configura Account Email",
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // VIP Manager Button
                val vipInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { showVipDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MeditechBorderCyan),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MeditechSurface),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                    modifier = Modifier.emilPressScale(vipInteraction, pressedScale = 0.92f)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MeditechCyanPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VIP (${uiState.vips.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MeditechCyanDark
                    )
                }
            }
        }

        // Apple Style Segmented Filter Pills
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MeditechGroupedBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Tutte" to "Tutte (${uiState.emails.size})",
                    "VIP SUPSI" to "VIP SUPSI (${uiState.emails.count { it.isVip }})",
                    "Con Meeting" to "Con Meeting (${uiState.emails.count { it.hasAppointmentProposal }})"
                ).forEach { (filterKey, filterLabel) ->
                    val isSelected = selectedFilter == filterKey
                    val chipInteraction = remember { MutableInteractionSource() }

                    Surface(
                        onClick = { selectedFilter = filterKey },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) MeditechSurface else Color.Transparent,
                        shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .emilPressScale(chipInteraction, pressedScale = 0.95f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MeditechCyanDark else MeditechTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Email Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredEmails, key = { it.id }) { email ->
                EmailCard(
                    email = email,
                    isSending = email.id in uiState.sentEmailIds,
                    onClick = { viewModel.openEmailDetail(email) },
                    onGenerateDraft = { viewModel.generateReplyDraft(email) },
                    onSendReply = { viewModel.sendEmailReply(email, email.draftReply) },
                    onCopyDraft = {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "Bozza copiata negli appunti", Toast.LENGTH_SHORT).show()
                    },
                    onAddToAgenda = {
                        viewModel.addMeetingFromEmail(
                            title = email.subject,
                            slotStr = email.suggestedSlot,
                            durationMinutes = email.suggestedDurationMinutes,
                            contact = email.sender.substringBefore('<').trim()
                        )
                    }
                )
            }
        }
    }

    // Modal Sheet Configurazione Email
    if (uiState.showEmailConfigSheet) {
        EmailAccountConfigSheet(
            config = uiState.emailAccountConfig,
            onSaveConfig = { viewModel.saveEmailAccountConfig(it) },
            onTestConnection = { viewModel.testEmailConnection(it) },
            onDismiss = { viewModel.setEmailConfigSheetVisible(false) }
        )
    }

    // Modal Sheet Dettaglio Email & Recap Intelligente
    uiState.selectedEmailDetail?.let { detailEmail ->
        EmailDetailSheet(
            email = detailEmail,
            onAddToAgenda = { title, slotStr, duration, contact ->
                viewModel.addMeetingFromEmail(title, slotStr, duration, contact)
            },
            onSendDraft = { replyText ->
                viewModel.sendEmailReply(detailEmail, replyText)
            },
            onCopyDraft = { replyText ->
                clipboardManager.setText(AnnotatedString(replyText))
                Toast.makeText(context, "Bozza copiata negli appunti", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.dismissEmailDetail() }
        )
    }

    // VIP Management Dialog
    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = { showVipDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MeditechCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contatti VIP SUPSI & EOC", fontWeight = FontWeight.Bold, color = MeditechTextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "I contatti VIP ricevono priorità immediata, sintesi automatica e notifiche dedicate da Qwen 3.5.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newVipName,
                        onValueChange = { newVipName = it },
                        label = { Text("Nome e Titolo (es. Dr. Rossi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newVipEmail,
                        onValueChange = { newVipEmail = it },
                        label = { Text("Email o Dominio (@eoc.ch, @supsi.ch)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newVipRole,
                        onValueChange = { newVipRole = it },
                        label = { Text("Ruolo Clinico / Istituzionale") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "VIP ATTIVI (${uiState.vips.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeditechCyanDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    uiState.vips.forEach { vip ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vip.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${vip.emailOrDomain} • ${vip.roleTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeditechTextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeVipContact(vip.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rimuovi",
                                    tint = MeditechUrgentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVipName.isNotBlank() && newVipEmail.isNotBlank()) {
                            viewModel.addVipContact(newVipName, newVipEmail, newVipRole)
                            newVipName = ""
                            newVipEmail = ""
                            newVipRole = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary)
                ) {
                    Text("Aggiungi VIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVipDialog = false }) {
                    Text("Chiudi", color = MeditechTextTertiary)
                }
            }
        )
    }
}

@Composable
fun EmailCard(
    email: EmailItem,
    isSending: Boolean = false,
    onClick: () -> Unit,
    onGenerateDraft: () -> Unit,
    onSendReply: () -> Unit,
    onCopyDraft: (String) -> Unit,
    onAddToAgenda: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var isAddedToAgenda by remember { mutableStateOf(false) }

    val cardInteraction = remember { MutableInteractionSource() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MeditechSurface),
        border = BorderStroke(1.dp, if (!email.isRead) MeditechCyanPrimary else MeditechBorder),
        modifier = Modifier
            .fillMaxWidth()
            .emilPressScale(cardInteraction, pressedScale = 0.985f)
            .clickable(interactionSource = cardInteraction, indication = null, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Unread Dot + Sender + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!email.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MeditechCyanPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }

                    Text(
                        text = email.sender.substringBefore('<').trim(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (email.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanSoft
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MeditechCyanDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = email.dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeditechTextTertiary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subject Line
            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MeditechTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body Snippet Preview
            Text(
                text = email.bodySnippet,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MeditechTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // APPLE INTELLIGENCE RECAP BOX (In-Card Highlight)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MeditechCyanUltraLight,
                border = BorderStroke(1.dp, MeditechBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Recap Intelligente AI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MeditechCyanDark
                                )
                            )
                        }

                        Text(
                            text = "Tocca per dettagli",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MeditechCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = formatMeditechMessageText(email.summary, isUser = false),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = MeditechTextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Meeting Quick Add Action if proposed
                    if (email.hasAppointmentProposal && email.suggestedSlot.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MeditechCyanDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Meeting: ${email.suggestedSlot}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MeditechCyanDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val agendaInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = {
                                    if (!isAddedToAgenda) {
                                        onAddToAgenda()
                                        isAddedToAgenda = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAddedToAgenda) MeditechTealSoft else MeditechCyanSoft,
                                modifier = Modifier.emilPressScale(agendaInteraction, pressedScale = 0.92f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAddedToAgenda) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAddedToAgenda) MeditechTealActive else MeditechCyanDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAddedToAgenda) "Inserito!" else "+ In Agenda",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isAddedToAgenda) MeditechTealActive else MeditechCyanDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
