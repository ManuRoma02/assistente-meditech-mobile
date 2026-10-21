package com.example.assistentemeditechmobile.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.MeditechTab
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistenteMeditechApp(viewModel: MeditechViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 3 Pagine: 0 = Email, 1 = AI Chat (default center!), 2 = Agenda Impegni
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    var inputFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Launcher per comandi vocali Android con fallback affidabile
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputFieldValue = TextFieldValue(spokenText, selection = TextRange(spokenText.length))
                viewModel.sendUserChatMessage(spokenText)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }
        }
    }

    val triggerVoiceInput: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla ora... (es: 'Fissa incontro con Stéphane Meystre')")
        }
        speechLauncher.launch(intent)
    }

    // Sincronizzazione swipe orizzontale Pager -> ViewModel Tab
    LaunchedEffect(pagerState.currentPage) {
        val tab = when (pagerState.currentPage) {
            0 -> MeditechTab.EMAIL_HUB
            2 -> MeditechTab.AGENDA
            else -> MeditechTab.CHAT_AI
        }
        if (uiState.currentTab != tab) {
            viewModel.selectTab(tab)
        }
    }

    // Dialog Aggiornamento Git OTA
    if (uiState.isUpdateAvailable || uiState.isDownloadingUpdate) {
        val manifest = uiState.updateManifest
        AlertDialog(
            onDismissRequest = { if (!uiState.isDownloadingUpdate) viewModel.dismissUpdateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aggiornamento Git MeDiTech",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Nuova versione disponibile: v${manifest?.latestVersionName ?: "1.2.0"} (Build #${manifest?.latestVersionCode ?: 3})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MeditechCyanDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = manifest?.releaseNotes?.ifBlank { "Miglioramenti all'interfaccia unificata, modello locale on-device, resoconto email automatico e docking bar." } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
                    )
                    if (uiState.isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Download da Git in corso... ${(uiState.updateProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MeditechCyanPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uiState.updateProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MeditechCyanPrimary,
                            trackColor = MeditechCyanSoft
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!viewModel.otaUpdateManager.canRequestPackageInstalls()) {
                            context.startActivity(viewModel.otaUpdateManager.createManageUnknownAppSourcesIntent())
                        } else {
                            viewModel.startOtaDownload { apkFile ->
                                context.startActivity(viewModel.otaUpdateManager.createInstallIntent(apkFile))
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                    enabled = !uiState.isDownloadingUpdate
                ) {
                    Text(if (uiState.isDownloadingUpdate) "Scaricamento..." else "Aggiorna da Git (1-Tap)")
                }
            },
            dismissButton = {
                if (!uiState.isDownloadingUpdate) {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Più tardi", color = MeditechTextSecondary)
                    }
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Foglio Modale Impostazioni
    if (uiState.showSettingsDialog) {
        MeditechSettingsSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setSettingsDialogVisible(false) }
        )
    }

    // Foglio Modale Azioni Rapide Cliniche (aperto dal tasto [+])
    if (showAttachSheet) {
        ClinicalActionsSheet(
            onSelectAction = { text ->
                inputFieldValue = TextFieldValue(text, selection = TextRange(text.length))
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
            },
            onSynthesizeEmails = {
                viewModel.postEmailBriefingToChat()
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
            },
            onClearChat = {
                showClearConfirmDialog = true
            },
            onDismiss = { showAttachSheet = false }
        )
    }

    // Dialog Conferma Ripulisci Cronologia
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MeditechUrgentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ripulisci Cronologia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeditechTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Vuoi dissolvere e ripulire tutti i messaggi della sessione corrente? I dati dell'agenda e le email non verranno toccati.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeditechTextSecondary
                )
            },
            confirmButton = {
                val delInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearChatHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechUrgentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cartoonPressSquash(delInteraction, pressedScaleY = 0.90f)
                ) {
                    Text("Dissolvi e Pulisci", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Annulla", color = MeditechTextSecondary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            MeditechHeader(
                currentPage = pagerState.currentPage,
                isAiProcessing = uiState.isAiProcessing,
                selectedModel = uiState.selectedAiModel,
                statusBanner = uiState.statusBanner,
                onDismissBanner = { viewModel.dismissBanner() },
                onOpenSettings = { viewModel.setSettingsDialogVisible(true) },
                onClearChat = { showClearConfirmDialog = true }
            )
        },
        bottomBar = {
            val currentProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 2f)
            UnifiedMeditechBottomBar(
                currentProgress = currentProgress,
                currentPage = pagerState.currentPage,
                onSelectPage = { page ->
                    val tab = when (page) {
                        0 -> MeditechTab.EMAIL_HUB
                        2 -> MeditechTab.AGENDA
                        else -> MeditechTab.CHAT_AI
                    }
                    viewModel.selectTab(tab)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                emailCount = uiState.emails.count { !it.isRead },
                agendaCount = uiState.events.size,
                inputFieldValue = inputFieldValue,
                onInputFieldValueChange = { inputFieldValue = it },
                onSendMessage = { text ->
                    viewModel.sendUserChatMessage(text)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onVoiceMicClick = triggerVoiceInput,
                onOpenActionsSheet = { showAttachSheet = true },
                isListening = uiState.isVoiceListening,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = MeditechBg
    ) { innerPadding ->
        // Swipeable HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            // Cartoony parallax & subtle depth during swipe
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - (abs(pageOffset) * 0.04f).coerceIn(0f, 0.08f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (abs(pageOffset) * 0.25f).coerceIn(0f, 0.4f)
                    }
            ) {
                when (page) {
                    0 -> EmailHubScreen(viewModel = viewModel)
                    1 -> MeditechAiChatScreen(viewModel = viewModel)
                    2 -> AgendaClinicaScreen(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Header Studio compatto ed elegante con 3D Neural Orb, branding SUPSI MeDiTech,
 * pillola di stato modello locale on-device, tasto cestino e ingranaggio impostazioni.
 * Non contiene più le tab (che vivono ora in basso nel contenitore di scrittura unificato).
 */
@Composable
fun MeditechHeader(
    currentPage: Int,
    isAiProcessing: Boolean,
    selectedModel: String,
    statusBanner: String?,
    onDismissBanner: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearChat: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeditechSurface)
            .statusBarsPadding()
    ) {
        // Riga Principale
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Interactive 3D Neural Thinking Orb
                Meditech3DNeuralOrb(
                    isProcessing = isAiProcessing,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MeDiTech AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanSoft
                        ) {
                            Text(
                                text = "SUPSI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MeditechCyanDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Istituto MeDiTech • Polo Clinico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTextTertiary
                    )
                }
            }

            // Right Actions: Live Local Model Capsule + Clear Trash Icon (chat only) + Settings Gear Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                val modelInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = MeditechTealSoft,
                    modifier = Modifier.cartoonPressSquash(modelInteraction, pressedScaleY = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MeditechTealActive, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (selectedModel.contains("2B")) "Qwen 3.5 2B" else if (selectedModel.contains("4B")) "Qwen 3.5 4B" else "Qwen Locale",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTealActive,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (currentPage == 1 && onClearChat != null) {
                    val clearInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MeditechGroupedBg, CircleShape)
                            .cartoonPressSquash(clearInteraction, pressedScaleY = 0.88f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Pulisci Cronologia Chat",
                            tint = MeditechUrgentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                val settingsInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MeditechGroupedBg, CircleShape)
                        .cartoonPressSquash(settingsInteraction, pressedScaleY = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Impostazioni Motore IA Locale",
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Animated Notification Banner
        AnimatedVisibility(
            visible = statusBanner != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (statusBanner != null) {
                Surface(
                    color = MeditechCyanSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MeditechCyanDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = MeditechCyanDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MeditechBorder, thickness = 0.8.dp)
    }
}

/**
 * UNIFIED MEDITECH BOTTOM BAR (STUDIO ARCHITETTURA UNIFICATA)
 * Contenitore unificato posto in basso: ospita la barra di scrittura con il [+]
 * e il docking a 3 sezioni (Email, Assistente AI, Impegni) nello stesso elemento.
 */
@Composable
fun UnifiedMeditechBottomBar(
    currentProgress: Float,
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    emailCount: Int,
    agendaCount: Int,
    inputFieldValue: TextFieldValue,
    onInputFieldValueChange: (TextFieldValue) -> Unit,
    onSendMessage: (String) -> Unit,
    onVoiceMicClick: () -> Unit,
    onOpenActionsSheet: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottomPx > 0
    val hasText = inputFieldValue.text.isNotBlank()

    Surface(
        color = Color(0xFBF8FAFA),
        border = BorderStroke(
            0.8.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.25f),
                    MeditechBorderCyan.copy(alpha = 0.45f)
                )
            )
        ),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Riga 1: Barra di scrittura con [+] e morphing Mic/Send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [+] Pulsante Azioni Cliniche Frosted Glass
                val attachInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenActionsSheet()
                    },
                    interactionSource = attachInteraction,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(
                        0.8.dp,
                        Brush.verticalGradient(
                            listOf(Color.White, MeditechBorderCyan.copy(alpha = 0.65f))
                        )
                    ),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .cartoonPressSquash(attachInteraction, pressedScaleY = 0.90f, squashScaleX = 1.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Azioni Cliniche Rapide",
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Campo di testo arrotondato Capsule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 110.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, Color(0xFFF7FCFC))
                            )
                        )
                        .border(
                            0.8.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    MeditechBorderCyan.copy(alpha = 0.50f)
                                )
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputFieldValue.text.isEmpty()) {
                        Text(
                            text = "Messaggio o comando clinico...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MeditechTextTertiary,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = inputFieldValue,
                        onValueChange = onInputFieldValueChange,
                        textStyle = TextStyle(
                            color = MeditechTextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        cursorBrush = SolidColor(MeditechCyanPrimary),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Pulsante Dinamico: Invia (se c'è testo) / Microfono (se vuoto)
                val sendInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        if (hasText) {
                            val toSend = inputFieldValue.text.trim()
                            if (toSend.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInputFieldValueChange(TextFieldValue(""))
                                onSendMessage(toSend)
                            }
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onVoiceMicClick()
                        }
                    },
                    interactionSource = sendInteraction,
                    shape = CircleShape,
                    color = MeditechCyanPrimary,
                    shadowElevation = 2.5.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .cartoonAcousticPulse(isListening && !hasText)
                        .cartoonPressSquash(sendInteraction, pressedScaleY = 0.88f, squashScaleX = 1.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = hasText,
                            transitionSpec = {
                                (scaleIn(
                                    initialScale = 0.84f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(tween(260))).togetherWith(
                                    scaleOut(
                                        targetScale = 0.84f,
                                        animationSpec = tween(200)
                                    ) + fadeOut(tween(200))
                                )
                            },
                            label = "mic_send_morph"
                        ) { showSend ->
                            if (showSend) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Invia",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Detta nota vocale",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Riga 2: Segmented Switcher a 3 Sezioni (Liquid Fluid Tab Dock)
            // Quando la tastiera è aperta (imeBottomPx > 0), nascondiamo la barra dock all'istante
            if (!isKeyboardOpen) {
                LiquidFluidTabDock(
                    currentProgress = currentProgress,
                    currentPage = currentPage,
                    onSelectPage = onSelectPage,
                    emailCount = emailCount,
                    agendaCount = agendaCount
                )
            }
        }
    }
}

/**
 * LIQUID FLUID TAB DOCK (3 SEZIONI SULLO STESSO LIVELLO DI PERITO AI)
 * Fisica a molla organica (/emilkowalski-animation): scivolamento continuo della pillola
 * su GPU RenderThread sincronizzato sia con swipe gesture sia con tap diretto.
 */
@Composable
fun LiquidFluidTabDock(
    currentProgress: Float,
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    emailCount: Int,
    agendaCount: Int,
    modifier: Modifier = Modifier
) {
    // Binario incassato con finitura frosted glass e bordi morbidi
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE9F1F3),
        border = BorderStroke(
            0.6.dp,
            Brush.verticalGradient(
                listOf(
                    MeditechBorderCyan.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.70f)
                )
            )
        ),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val availableWidth = maxWidth
            val tabWidth = availableWidth / 3

            // Pillola Bianca morbida che scorre su GPU RenderThread
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = (currentProgress * tabWidth.toPx())
                    }
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(13.dp),
                        ambientColor = Color(0x20000000),
                        spotColor = Color(0x15000000)
                    )
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF9FCFC)
                            )
                        )
                    )
                    .border(
                        width = 0.6.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.40f)
                            )
                        ),
                        shape = RoundedCornerShape(13.dp)
                    )
            )

            // Livello etichette ed icone delle 3 sezioni
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 0: Email
                TabDockItem(
                    index = 0,
                    label = "Email",
                    badgeCount = emailCount,
                    isSelected = currentPage == 0,
                    selectedIcon = Icons.Default.Mail,
                    unselectedIcon = Icons.Outlined.MailOutline,
                    currentProgress = currentProgress,
                    onClick = { onSelectPage(0) },
                    modifier = Modifier.weight(1f)
                )

                // 1: Assistente AI
                TabDockItem(
                    index = 1,
                    label = "Assistente",
                    badgeCount = 0,
                    isSelected = currentPage == 1,
                    selectedIcon = Icons.Default.ChatBubble,
                    unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                    currentProgress = currentProgress,
                    onClick = { onSelectPage(1) },
                    modifier = Modifier.weight(1f)
                )

                // 2: Impegni
                TabDockItem(
                    index = 2,
                    label = "Impegni",
                    badgeCount = agendaCount,
                    isSelected = currentPage == 2,
                    selectedIcon = Icons.Default.CalendarMonth,
                    unselectedIcon = Icons.Outlined.CalendarToday,
                    currentProgress = currentProgress,
                    onClick = { onSelectPage(2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TabDockItem(
    index: Int,
    label: String,
    badgeCount: Int,
    isSelected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    currentProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val fraction = (1f - abs(currentProgress - index)).coerceIn(0f, 1f)
    val itemColor = lerp(MeditechTextSecondary, MeditechCyanDark, fraction)
    val interaction = remember { MutableInteractionSource() }

    val iconBounce by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tab_icon_bounce_$index"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .cartoonPressSquash(interaction, pressedScaleY = 0.93f, squashScaleX = 1.04f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = null,
                tint = itemColor,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = iconBounce
                        scaleY = iconBounce
                    }
            )

            Spacer(Modifier.width(5.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = itemColor,
                maxLines = 1
            )

            if (badgeCount > 0) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MeditechCyanPrimary else MeditechCyanSoft,
                    modifier = Modifier.size(17.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) Color.White else MeditechCyanDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Foglio Modale Azioni Cliniche Rapide
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalActionsSheet(
    onSelectAction: (String) -> Unit,
    onSynthesizeEmails: () -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = MeditechSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MeditechBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "AZIONI CLINICHE RAPIDE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            listOf(
                Triple("Nuovo Incontro / Visita", Icons.Default.Event, "Fissa incontro clinico con Prof. Stéphane Meystre"),
                Triple("Nuova Nota Clinica", Icons.Default.NoteAdd, "Registra nota clinica per il dossier: "),
                Triple("Sintetizza Email Ricevute", Icons.Default.MarkEmailRead, "Briefing email dall'agente locale"),
                Triple("Ripulisci Cronologia Chat", Icons.Default.DeleteSweep, "Dissolvi i messaggi della sessione")
            ).forEach { (title, icon, actionPayload) ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        onDismiss()
                        when (title) {
                            "Sintetizza Email Ricevute" -> onSynthesizeEmails()
                            "Ripulisci Cronologia Chat" -> onClearChat()
                            else -> onSelectAction(actionPayload)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MeditechGroupedBg,
                    border = BorderStroke(0.8.dp, MeditechBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .cartoonPressSquash(interaction, pressedScaleY = 0.94f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (title.contains("Ripulisci")) MeditechUrgentRed.copy(alpha = 0.15f) else MeditechCyanSoft,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (title.contains("Ripulisci")) MeditechUrgentRed else MeditechCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (title.contains("Ripulisci")) MeditechUrgentRed else MeditechTextPrimary
                        )
                    }
                }
            }
        }
    }
}
