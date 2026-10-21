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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            color = MeditechTextPrimary
                        )
                        Text(
                            text = "Configurazione motore clinico & preferenze",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechTextTertiary
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

            // 1. Modello IA
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MeditechCyanDark else MeditechTextPrimary
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setAiModel(modelName) },
                            colors = RadioButtonDefaults.colors(selectedColor = MeditechCyanPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MeditechCyanPrimary else MeditechGroupedBg,
                        border = BorderStroke(1.dp, if (isSelected) MeditechCyanPrimary else MeditechBorder),
                        modifier = Modifier
                            .weight(1f)
                            .emilPressScale(interaction)
                    ) {
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MeditechTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Preferenze Agenda & Meeting
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
                                color = MeditechTextPrimary
                            )
                            Text(
                                text = "Genera videochiamata istantanea per riunioni online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
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
                                color = MeditechTextPrimary
                            )
                            Text(
                                text = "Evita accavallamenti tra visite e impegni clinici",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
                        Switch(
                            checked = uiState.safetyBufferEnabled,
                            onCheckedChange = { viewModel.toggleSafetyBuffer(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MeditechCyanPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Aggiornamenti OTA Git
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
                    text = "Verifica Aggiornamenti Git (v${viewModel.otaUpdateManager.getCurrentVersionName()} • Build #${viewModel.otaUpdateManager.getCurrentVersionCode()})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MeditechCyanPrimary,
                    fontWeight = FontWeight.Bold
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
