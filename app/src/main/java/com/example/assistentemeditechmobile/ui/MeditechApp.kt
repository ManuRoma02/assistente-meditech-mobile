package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // 3 Pages: 0 = Email, 1 = AI Chat (default center!), 2 = Agenda Impegni
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

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

    // Sincronizzazione ViewModel Tab -> Pager (1-Tap & Programmatic Navigation)
    LaunchedEffect(uiState.currentTab) {
        val targetPage = when (uiState.currentTab) {
            MeditechTab.EMAIL_HUB -> 0
            MeditechTab.AGENDA -> 2
            else -> 1
        }
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // OTA Update Dialog
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
                        text = manifest?.releaseNotes?.ifBlank { "Miglioramenti all'agenda clinica, sincronizzazione email, gesture a scorrimento orizzontale e motore IA." } ?: "",
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

    // Settings BottomSheet
    if (uiState.showSettingsDialog) {
        MeditechSettingsSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setSettingsDialogVisible(false) }
        )
    }

    Scaffold(
        topBar = {
            MeditechHeader(
                currentPage = pagerState.currentPage,
                onTabClick = { page ->
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
                isAiProcessing = uiState.isAiProcessing,
                selectedModel = uiState.selectedAiModel,
                statusBanner = uiState.statusBanner,
                onDismissBanner = { viewModel.dismissBanner() },
                onOpenSettings = { viewModel.setSettingsDialogVisible(true) },
                onClearChat = { viewModel.clearChatHistory() }
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
            // Cartoony parallax transform during swipe
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
 * Header unificato con 3D Neural Orb, branding SUPSI, badge modello,
 * tasto Impostazioni e barra di selezione a 3 pillole animate con spring physics.
 */
@Composable
fun MeditechHeader(
    currentPage: Int,
    onTabClick: (Int) -> Unit,
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
        // Riga 1: 3D Orb + MeDiTech Title + Model Badge + Settings Gear
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

            // Right Actions: Live Model Capsule + Settings Gear Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = MeditechTealSoft
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
                            text = selectedModel.substringBefore(" ").take(10),
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
                            .emilPressScale(clearInteraction, pressedScale = 0.88f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Pulisci Cronologia",
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
                        .emilPressScale(settingsInteraction)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Impostazioni Motore IA",
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Riga 2: 3 Segmented Pill Selector (Email | Assistente AI | Impegni)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MeditechGroupedBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    0 to "Email",
                    1 to "Assistente AI",
                    2 to "Impegni"
                ).forEach { (pageIndex, label) ->
                    val isSelected = currentPage == pageIndex
                    val interaction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isSelected) MeditechCyanPrimary else Color.Transparent)
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                onTabClick(pageIndex)
                            }
                            .emilPressScale(interaction, pressedScale = 0.92f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MeditechTextSecondary
                        )
                    }
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

        HorizontalDivider(color = MeditechBorder, thickness = 1.dp)
    }
}
