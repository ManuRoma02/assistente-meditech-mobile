package com.example.assistentemeditechmobile.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistentemeditechmobile.domain.EmailItem
import com.example.assistentemeditechmobile.domain.VipContact
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Schermata Hub Email Cliniche & Ricerca MeDiTech SUPSI:
 * - Filtri Apple HIG con contatori dinamici
 * - Card email ad altissima leggibilità con avatar iniziali, badge VIP e sintesi on-device
 * - Rilevamento automatico meeting con 1-tap add to calendar
 * - Bozze di risposta IA rapide con animazione cartoon paper plane
 * - SwipeToDismissBox per segna-letta / elimina rapida
 * - PullToRefreshBox per sincronizzazione immediata
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailHubScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
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
            "Non Lette" -> uiState.emails.filter { !it.isRead }
            else -> uiState.emails
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 14.dp)
    ) {
        // Top Header: Title + Actions (Simulate, Config, VIPs, Briefing)
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MeditechCyanPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Posta in Arrivo",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp
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
                // Simula Arrivo Nuova Email
                val syncInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.simulateReceiveIncomingEmail()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MeditechCyanSoft, CircleShape)
                        .cartoonPressSquash(syncInteraction, pressedScaleY = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = "Simula Nuova Email",
                        tint = MeditechCyanDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Configurazione Account IMAP/Exchange
                val configInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setEmailConfigSheetVisible(true)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MeditechGroupedBg, CircleShape)
                        .cartoonPressSquash(configInteraction, pressedScaleY = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = "Configura Account Email",
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // VIP Manager Button
                val vipInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showVipDialog = true
                    },
                    interactionSource = vipInteraction,
                    shape = RoundedCornerShape(12.dp),
                    color = MeditechCyanSoft,
                    border = BorderStroke(0.8.dp, MeditechBorderCyan),
                    modifier = Modifier.cartoonPressSquash(vipInteraction, pressedScaleY = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MeditechCyanDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "VIP (${uiState.vips.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MeditechCyanDark
                        )
                    }
                }
            }
        }

        // Apple Style Segmented Filter Pills
        Surface(
            shape = RoundedCornerShape(14.dp),
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
                    "VIP SUPSI" to "VIP (${uiState.emails.count { it.isVip }})",
                    "Con Meeting" to "Meeting (${uiState.emails.count { it.hasAppointmentProposal }})",
                    "Non Lette" to "Nuove (${uiState.emails.count { !it.isRead }})"
                ).forEach { (filterKey, filterLabel) ->
                    val isSelected = selectedFilter == filterKey
                    val chipInteraction = remember { MutableInteractionSource() }

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = filterKey
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MeditechSurface else Color.Transparent,
                        border = if (isSelected) BorderStroke(0.6.dp, MeditechBorderCyan) else null,
                        shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .cartoonPressSquash(chipInteraction, pressedScaleY = 0.94f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Lista Email o Stato Vuoto con Pull-To-Refresh e SwipeToDismiss
        PullToRefreshBox(
            isRefreshing = uiState.isEmailRefreshing,
            onRefresh = { viewModel.refreshEmailPull() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredEmails.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MeditechSurface,
                        border = BorderStroke(0.8.dp, MeditechBorderCyan),
                        shadowElevation = 1.dp,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MeditechCyanSoft,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MailOutline,
                                        contentDescription = null,
                                        tint = MeditechCyanPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Nessuna email presente",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MeditechTextPrimary
                            )
                            Text(
                                text = "Nessuna email inventata di default. Collega la tua casella IMAP/Exchange o simula l'arrivo di una comunicazione con il pulsante in alto.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MeditechTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val configInteraction = remember { MutableInteractionSource() }
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setEmailConfigSheetVisible(true)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MeditechCyanPrimary),
                                    modifier = Modifier.emilPressScale(configInteraction, pressedScale = 0.95f)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = MeditechCyanPrimary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Configura Casella", fontSize = 12.sp, color = MeditechCyanPrimary, fontWeight = FontWeight.SemiBold)
                                }

                                val simInteraction = remember { MutableInteractionSource() }
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.simulateReceiveIncomingEmail()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                                    modifier = Modifier.cartoonPressSquash(simInteraction, pressedScaleY = 0.92f)
                                ) {
                                    Icon(Icons.Default.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simula Ricezione", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredEmails, key = { it.id }) { email ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.markEmailAsRead(email.id)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteEmail(email.id)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> MeditechCyanPrimary
                                    SwipeToDismissBoxValue.EndToStart -> MeditechUrgentRed
                                    else -> Color.Transparent
                                }
                                val alignment = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }
                                val icon = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.MarkEmailRead
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    else -> null
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            },
                            content = {
                                EmailCard(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                                        placementSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                                        fadeOutSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                    ),
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
                        )
                    }
                }
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

    // Sheet Dettaglio Email
    uiState.selectedEmailDetail?.let { email ->
        EmailDetailSheet(
            email = email,
            onAddToAgenda = { title, slotStr, durationMinutes, contact ->
                viewModel.addMeetingFromEmail(title, slotStr, durationMinutes, contact)
            },
            onSendDraft = { draft ->
                viewModel.sendEmailReply(email, draft)
            },
            onCopyDraft = { draft ->
                clipboardManager.setText(AnnotatedString(draft))
                Toast.makeText(context, "Bozza copiata negli appunti", Toast.LENGTH_SHORT).show()
            },
            onDeleteEmail = { viewModel.deleteEmail(it) },
            onDismiss = { viewModel.dismissEmailDetail() }
        )
    }

    // Dialog Gestione Contatti VIP
    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = { showVipDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contatti VIP MeDiTech",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "I mittenti VIP ricevono priorità assoluta, notifiche dedicate e analisi anticipata dell'agenda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.vips.forEach { vip ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MeditechGroupedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vip.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MeditechTextPrimary
                                    )
                                    Text(
                                        text = "${vip.emailOrDomain} • ${vip.roleTitle}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MeditechTextTertiary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeVipContact(vip.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Rimuovi VIP",
                                        tint = MeditechUrgentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aggiungi nuovo VIP:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MeditechCyanDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newVipName,
                        onValueChange = { newVipName = it },
                        placeholder = { Text("Nome (es: Prof. Stéphane Meystre)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newVipEmail,
                        onValueChange = { newVipEmail = it },
                        placeholder = { Text("Email o dominio (es: stephane.meystre@supsi.ch)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newVipRole,
                        onValueChange = { newVipRole = it },
                        placeholder = { Text("Ruolo (es: Direttore Istituto)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVipName.isNotBlank() && newVipEmail.isNotBlank()) {
                            viewModel.addVipContact(newVipName, newVipEmail, newVipRole.ifBlank { "Collaboratore Scientifico" })
                            newVipName = ""
                            newVipEmail = ""
                            newVipRole = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Salva VIP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVipDialog = false }) {
                    Text("Chiudi", color = MeditechTextSecondary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Card Email in stile Apple HIG:
 * - Avatar con iniziali mittente
 * - Bordo luminescente per VIP Meystre
 * - Box Apple Intelligence con sintesi formattata on-device
 * - Azione rapida Meeting in Agenda
 */
@Composable
fun EmailCard(
    email: EmailItem,
    isSending: Boolean = false,
    onClick: () -> Unit,
    onGenerateDraft: () -> Unit,
    onSendReply: () -> Unit,
    onCopyDraft: (String) -> Unit,
    onAddToAgenda: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isAddedToAgenda by remember { mutableStateOf(false) }

    val cardInteraction = remember { MutableInteractionSource() }

    // Iniziali mittente per avatar
    val initials = remember(email.sender) {
        val clean = email.sender.substringBefore('<').trim()
        val parts = clean.split(' ').filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ' '}${parts[1].firstOrNull() ?: ' '}".uppercase()
        } else {
            clean.take(2).uppercase()
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeditechSurface,
        border = if (email.isVip) {
            BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(MeditechCyanPrimary, VipMeystreGlow)))
        } else if (!email.isRead) {
            BorderStroke(1.dp, MeditechCyanPrimary)
        } else {
            BorderStroke(0.8.dp, MeditechBorder)
        },
        shadowElevation = if (!email.isRead || email.isVip) 2.dp else 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .cartoonPaperPlane(isSending)
            .cartoonPressSquash(cardInteraction, pressedScaleY = 0.985f)
            .clickable(interactionSource = cardInteraction, indication = null, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar Iniziali + Mittente + Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // Avatar Iniziali
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (email.isVip) Brush.linearGradient(listOf(MeditechCyanPrimary, MeditechCyanDark))
                                else Brush.linearGradient(listOf(MeditechGroupedBg, MeditechBorder))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (email.isVip) Color.White else MeditechTextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = email.sender.substringBefore('<').trim(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = MeditechTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (email.isVip) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MeditechCyanSoft
                                ) {
                                    Text(
                                        text = "VIP SUPSI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MeditechCyanDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (!email.isRead) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MeditechCyanPrimary)
                                )
                                Text(
                                    text = "Non Letta",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MeditechCyanPrimary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = email.dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MeditechTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Oggetto
            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = MeditechTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Snippet Corpo Email
            Text(
                text = email.bodySnippet,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MeditechTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // BOX INTELLIGENCE ON-DEVICE ME-DITECH (Sintesi Formattata)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MeditechCyanUltraLight,
                border = BorderStroke(0.6.dp, MeditechBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Sintesi IA Locale",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = MeditechCyanDark
                            )
                        }

                        Text(
                            text = "Tocca per leggere",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MeditechCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatInlineMarkdown(email.summary, isUser = false),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        ),
                        color = MeditechTextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Meeting Quick Add Action se proposta presente
                    if (email.hasAppointmentProposal && email.suggestedSlot.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MeditechCyanDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Slot: ${email.suggestedSlot}",
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
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAddToAgenda()
                                        isAddedToAgenda = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAddedToAgenda) MeditechTealSoft else MeditechCyanSoft,
                                border = BorderStroke(
                                    0.6.dp,
                                    if (isAddedToAgenda) MeditechTealActive else MeditechCyanPrimary
                                ),
                                modifier = Modifier.cartoonPressSquash(agendaInteraction, pressedScaleY = 0.90f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAddedToAgenda) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAddedToAgenda) MeditechTealActive else MeditechCyanPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isAddedToAgenda) "In Agenda!" else "Fissa Slot",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
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
