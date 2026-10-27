package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailSheet(
    email: EmailItem,
    onAddToAgenda: (title: String, slotStr: String, durationMinutes: Int, contact: String) -> Unit,
    onSendDraft: (String) -> Unit,
    onCopyDraft: (String) -> Unit,
    onDeleteEmail: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isSending by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var isAddedToAgenda by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MeditechBg,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MeditechBorder,
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 38.dp, height = 4.5.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header Row: Account Pill + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MeditechCyanSoft
                ) {
                    Text(
                        text = if (email.language == "en") "${email.accountName} • EN" else email.accountName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MeditechCyanDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val deleteInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = {
                            onDeleteEmail(email.id)
                            onDismiss()
                        },
                        modifier = Modifier.emilPressScale(deleteInteraction)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Elimina Email",
                            tint = MeditechUrgentRed
                        )
                    }

                    val closeInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.emilPressScale(closeInteraction)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = MeditechTextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subject Line (Apple Title 2 Typography)
            SelectionContainer {
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        lineHeight = 27.sp
                    ),
                    color = MeditechTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sender & Date Card (Apple Inset Row)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MeditechSurface,
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Initials
                    val senderName = email.sender.substringBefore('<').trim()
                    val initials = senderName.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")
                    Surface(
                        shape = CircleShape,
                        color = if (email.isVip) MeditechCyanPrimary else MeditechGroupedBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials.ifBlank { "EM" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (email.isVip) Color.White else MeditechTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = senderName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MeditechTextPrimary,
                                maxLines = 1
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
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "VIP EOC/SUPSI",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = MeditechCyanDark
                                        )
                                    }
                                }
                            }
                        }

                        val senderEmail = email.sender.substringAfter('<', "").substringBefore('>', "")
                        if (senderEmail.isNotBlank()) {
                            Text(
                                text = senderEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = email.dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================================
            // APPLE INTELLIGENCE RECAP CARD (Frosted Glowing Executive Summary)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MeditechCyanUltraLight,
                border = BorderStroke(1.5.dp, MeditechBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Card Title with AI Sparkle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MeditechCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recap Intelligente • Qwen 3.5",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MeditechCyanDark,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = CircleShape,
                            color = MeditechTealSoft
                        ) {
                            Text(
                                text = "100% Locale",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = MeditechTealActive,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Formatted Summary Text
                    SelectionContainer {
                        Text(
                            text = formatMeditechMessageText(email.summary, isUser = false),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 21.sp
                            ),
                            color = MeditechTextPrimary
                        )
                    }

                    // Meeting Proposal Action (Apple Calendar Integration)
                    if (email.hasAppointmentProposal && email.suggestedSlot.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MeditechSurface,
                            border = BorderStroke(1.dp, if (isAddedToAgenda) MeditechTealActive else MeditechCyanPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MeditechCyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Proposta di Incontro Rilevata",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MeditechCyanDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Slot: ${email.suggestedSlot} (${email.suggestedDurationMinutes} min)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MeditechTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val addAgendaInteraction = remember { MutableInteractionSource() }
                                Button(
                                    onClick = {
                                        if (!isAddedToAgenda) {
                                            onAddToAgenda(
                                                email.subject,
                                                email.suggestedSlot,
                                                email.suggestedDurationMinutes,
                                                email.sender.substringBefore('<').trim()
                                            )
                                            isAddedToAgenda = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAddedToAgenda) MeditechTealActive else MeditechCyanPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .emilPressScale(addAgendaInteraction)
                                ) {
                                    Icon(
                                        imageVector = if (isAddedToAgenda) Icons.Default.Check else Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAddedToAgenda) "Inserito in Agenda Clinica!" else "Aggiungi ad Agenda Clinica (1-Tap)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // Draft Reply Box
                    if (email.draftReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "BOZZA DI RISPOSTA CLINICA RAPIDA",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MeditechCyanDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeditechSurface,
                            border = BorderStroke(1.dp, MeditechBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                SelectionContainer {
                                    Text(
                                        text = email.draftReply,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                        color = MeditechTextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Invia Subito
                                    val sendInteraction = remember { MutableInteractionSource() }
                                    Button(
                                        onClick = {
                                            if (!isSending) {
                                                isSending = true
                                                coroutineScope.launch {
                                                    delay(500)
                                                    onSendDraft(email.draftReply)
                                                    isSending = false
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .emilPressScale(sendInteraction)
                                    ) {
                                        Icon(
                                            imageVector = if (isSending) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSending) "Inviata!" else "Invia Subito",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // Copia Bozza
                                    val copyInteraction = remember { MutableInteractionSource() }
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(email.draftReply))
                                            isCopied = true
                                            coroutineScope.launch {
                                                delay(1800)
                                                isCopied = false
                                            }
                                            onCopyDraft(email.draftReply)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isCopied) MeditechTealActive else MeditechBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .emilPressScale(copyInteraction)
                                    ) {
                                        Icon(
                                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = if (isCopied) MeditechTealActive else MeditechTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCopied) "Copiata!" else "Copia",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isCopied) MeditechTealActive else MeditechTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // FULL EMAIL BODY
            // =========================================================================
            Text(
                text = "CORPO DEL MESSAGGIO ORIGINALE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MeditechSurface,
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = email.bodySnippet,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        color = MeditechTextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Attachments Section
            if (email.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ALLEGATI CLINICI (${email.attachments.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeditechCyanDark,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    email.attachments.forEach { filename ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeditechSurface,
                            border = BorderStroke(1.dp, MeditechBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MeditechUrgentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = filename,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MeditechTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Documento PDF verificato",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeditechTextTertiary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Scarica",
                                    tint = MeditechCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
