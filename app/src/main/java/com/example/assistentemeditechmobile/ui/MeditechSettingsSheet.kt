package com.example.assistentemeditechmobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditechSettingsSheet(
    uiState: MeditechUiState,
    viewModel: MeditechViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MeditechCyanSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Impostazioni MeDiTech AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Configurazione motore clinico & preferenze",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MeditechGroupedBg, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = MeditechTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Modello IA On-Device
            Text(
                text = "MODELLO ON-DEVICE (OFFLINE & PRIVACY CLINICA)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val models = listOf(
                "Qwen 3.5 - 2B HighIQ (Locale On-Device)" to "Consigliato • Modello 2B Q4_K_M (1.25 GB) 100% On-Device, zero cloud",
                "Qwen 3.5 - 4B Instruct (Locale On-Device)" to "Potenziato • Alta capacità logica per perizie e cartelle cliniche (2.81 GB)",
                "Rizzo Flow 1.7B (Decision Model)" to "Decision Model • Classificazione email e cartelle a 0 token generati"
            )

            models.forEach { (modelName, desc) ->
                val isSelected = uiState.selectedAiModel == modelName
                val interaction = remember { MutableInteractionSource() }

                Surface(
                    onClick = { viewModel.setAiModel(modelName) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MeditechCyanUltraLight else MeditechSurface,
                    border = BorderStroke(1.dp, if (isSelected) MeditechCyanPrimary else MeditechBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .emilPressScale(interaction)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MeditechCyanPrimary else MeditechBorder,
                            modifier = Modifier.size(12.dp)
                        ) {}

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MeditechCyanDark else MeditechTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                color = MeditechTextTertiary
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selezionato",
                                tint = MeditechCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Stile di Risposta
            Text(
                text = "STILE RISPOSTA & RAGIONAMENTO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Veloce & Diretto", "Clinico Bilanciato", "Approfondito").forEach { style ->
                    val isSelected = uiState.responseStyle == style
                    val interaction = remember { MutableInteractionSource() }

                    Surface(
                        onClick = { viewModel.setResponseStyle(style) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MeditechCyanSoft else MeditechGroupedBg,
                        border = BorderStroke(1.dp, if (isSelected) MeditechCyanPrimary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .emilPressScale(interaction)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MeditechCyanDark else MeditechTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Automazione Clinica & Agenda
            Text(
                text = "AUTOMAZIONE CLINICA & AGENDA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechGroupedBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link Google Meet Automatico",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MeditechTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Genera videochiamata istantanea per riunioni online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.autoCreateMeet,
                            onCheckedChange = { viewModel.toggleAutoCreateMeet(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MeditechCyanPrimary)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MeditechBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Buffer di Sicurezza (15 min)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MeditechTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Evita accavallamenti tra visite e impegni clinici",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.safetyBufferEnabled,
                            onCheckedChange = { viewModel.toggleSafetyBuffer(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MeditechCyanPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Configurazione Account Email (Apple Style)
            Text(
                text = "ACCOUNT EMAIL & SINCRONIZZAZIONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechGroupedBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanSoft,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MeditechCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.emailAccountConfig.emailAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MeditechTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.emailAccountConfig.provider} • Sincronizzazione IMAP SSL attiva",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeditechTextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val configBtnInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            viewModel.setEmailConfigSheetVisible(true)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MeditechCyanPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MeditechCyanPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .emilPressScale(configBtnInteraction)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configura Parametri Posta (IMAP / SMTP)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Aggiornamenti OTA Git
            Text(
                text = "STATO DEL SISTEMA & AGGIORNAMENTI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val updateInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    viewModel.checkForAppUpdate()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MeditechCyanPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .emilPressScale(updateInteraction)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MeditechCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verifica Aggiornamenti (v${viewModel.otaUpdateManager.getCurrentVersionName()} • Build #${viewModel.otaUpdateManager.getCurrentVersionCode()})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MeditechCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Box Sintetico Novità Aggiornamenti con Parole Chiave in Grassetto
            val latestNotes = uiState.updateManifest?.releaseNotes?.ifBlank { null }
                ?: "• **Docking Unificato**: navigazione fluida a 3 sezioni integrata in basso\n• **Modelli On-Device**: supporto esclusivo **Qwen 3.5** (rimosso **Qwen 2.5**)\n• **Markdown Nativo**: eliminati asterischi grezzi con resa tipografica reale\n• **Briefing Email**: sintesi automatica locale delle comunicazioni cliniche\n• **Aggiornamenti OTA**: verifica e installazione rapida **1-Tap** da Git"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MeditechSurface,
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Novità Versione v${viewModel.otaUpdateManager.getCurrentVersionName()}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MeditechCyanDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatMeditechMessageText(raw = latestNotes, isUser = false),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = MeditechTextSecondary
                    )
                }
            }
        }
    }
}
