package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.R
import com.example.assistentemeditechmobile.domain.MeditechTab
import com.example.assistentemeditechmobile.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistenteMeditechApp(viewModel: MeditechViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                        text = "Nuova versione disponibile: v${manifest?.latestVersionName ?: "1.0.1"} (Build #${manifest?.latestVersionCode ?: 2})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MeditechCyanDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = manifest?.releaseNotes?.ifBlank { "Miglioramenti all'agenda clinica, sincronizzazione email e comandi vocali." } ?: "",
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

    Scaffold(
        topBar = {
            MeditechTopBar(
                statusBanner = uiState.statusBanner,
                onDismissBanner = { viewModel.dismissBanner() }
            )
        },
        bottomBar = {
            MeditechBottomNav(
                currentTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = MeditechBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                MeditechTab.AGENDA -> AgendaClinicaScreen(viewModel = viewModel)
                MeditechTab.EMAIL_HUB -> EmailHubScreen(viewModel = viewModel)
                MeditechTab.PROGETTI -> ProgettiRicercaScreen(viewModel = viewModel)
                MeditechTab.CHAT_AI -> MeditechAiChatScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MeditechTopBar(
    statusBanner: String?,
    onDismissBanner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeditechSurface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Clinical Avatar / Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MeditechCyanSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.meditech_avatar),
                        contentDescription = "MeDiTech SUPSI Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                }

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

            // Status Indicator (Live ECG Teal dot)
            Surface(
                shape = CircleShape,
                color = MeditechTealSoft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(MeditechTealActive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ON-DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeditechTealActive,
                        fontSize = 10.sp
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi", tint = MeditechCyanDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MeditechBorder, thickness = 1.dp)
    }
}

@Composable
fun MeditechBottomNav(
    currentTab: MeditechTab,
    onTabSelected: (MeditechTab) -> Unit
) {
    Surface(
        color = MeditechSurface,
        border = BorderStroke(1.dp, MeditechBorder),
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentTab == MeditechTab.AGENDA,
                onClick = { onTabSelected(MeditechTab.AGENDA) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda") },
                label = { Text("Agenda", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeditechCyanPrimary,
                    selectedTextColor = MeditechCyanPrimary,
                    unselectedIconColor = MeditechTextTertiary,
                    unselectedTextColor = MeditechTextTertiary,
                    indicatorColor = MeditechCyanSoft
                )
            )

            NavigationBarItem(
                selected = currentTab == MeditechTab.EMAIL_HUB,
                onClick = { onTabSelected(MeditechTab.EMAIL_HUB) },
                icon = { Icon(Icons.Default.Email, contentDescription = "Email Hub") },
                label = { Text("Email", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeditechCyanPrimary,
                    selectedTextColor = MeditechCyanPrimary,
                    unselectedIconColor = MeditechTextTertiary,
                    unselectedTextColor = MeditechTextTertiary,
                    indicatorColor = MeditechCyanSoft
                )
            )

            NavigationBarItem(
                selected = currentTab == MeditechTab.PROGETTI,
                onClick = { onTabSelected(MeditechTab.PROGETTI) },
                icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Progetti") },
                label = { Text("Progetti", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeditechCyanPrimary,
                    selectedTextColor = MeditechCyanPrimary,
                    unselectedIconColor = MeditechTextTertiary,
                    unselectedTextColor = MeditechTextTertiary,
                    indicatorColor = MeditechCyanSoft
                )
            )

            NavigationBarItem(
                selected = currentTab == MeditechTab.CHAT_AI,
                onClick = { onTabSelected(MeditechTab.CHAT_AI) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "MeDiTech AI") },
                label = { Text("AI Chat", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeditechCyanPrimary,
                    selectedTextColor = MeditechCyanPrimary,
                    unselectedIconColor = MeditechTextTertiary,
                    unselectedTextColor = MeditechTextTertiary,
                    indicatorColor = MeditechCyanSoft
                )
            )
        }
    }
}
