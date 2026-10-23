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
import androidx.compose.runtime.*
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
    var localOnDevicePath by remember { mutableStateOf(uiState.onDeviceModelPath) }
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

            // 1. Privacy Clinica & Architettura 100% On-Device
            Text(
                text = "PROTEZIONE PRIVACY & DATI CLINICI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechTealActive,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechTealSoft),
                border = BorderStroke(1.dp, MeditechTealActive.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MeditechTealActive,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% On-Device Locale (Zero Cloud)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MeditechTealActive
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "I dati sanitari, le note dei pazienti, l'agenda con il Prof. Stéphane Meystre e la corrispondenza sono protetti e rimangono rigorosamente all'interno di questo smartphone. Nessuna API cloud esterna riceve informazioni cliniche, in conformità con la LPD Svizzera e il GDPR art. 9.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.5.sp),
                        color = MeditechTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Modello AI Locale: Unica Opzione Attiva (Qwen 3.5 2B 4-bit)
            Text(
                text = "MODELLO AI ATTIVO (UNICA OPZIONE CONSENTITA)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechGroupedBg),
                border = BorderStroke(1.5.dp, MeditechCyanPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MeditechCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.selectedAiModel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeditechCyanDark,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Attivo",
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Modello locale compatto quantizzato a 4-bit per architetture ARM64. Elaborazione neurale interamente sul processore dello smartphone (NPU/CPU) con zero emissione di token all'esterno.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = MeditechTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Specifiche tecniche
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MeditechSurface,
                            border = BorderStroke(0.8.dp, MeditechBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Quantizzazione", fontSize = 10.5.sp, color = MeditechTextTertiary)
                                Text("4-bit Q4_K_M", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MeditechTextPrimary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MeditechSurface,
                            border = BorderStroke(0.8.dp, MeditechBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Dimensione", fontSize = 10.5.sp, color = MeditechTextTertiary)
                                Text("~1.25 GB", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MeditechTextPrimary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MeditechSurface,
                            border = BorderStroke(0.8.dp, MeditechBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Isolamento", fontSize = 10.5.sp, color = MeditechTextTertiary)
                                Text("Zero Cloud", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MeditechTealActive)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Storage Pesi Modello Locale su Telefono
            Text(
                text = "STORAGE FILE PESI MODELLO (.GGUF / .BIN)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechGroupedBg),
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Percorso File su Memoria Smartphone:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MeditechTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Posiziona il file pesi quantizzato 'qwen3.5-2b-q4_k_m.gguf' nella memoria interna.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MeditechTextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = localOnDevicePath,
                        onValueChange = {
                            localOnDevicePath = it
                            viewModel.updateOnDeviceModelPath(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        placeholder = { Text("es: /sdcard/Download/qwen3.5-2b-q4_k_m.gguf", fontSize = 12.sp) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val testPathInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            viewModel.updateOnDeviceModelPath(localOnDevicePath)
                            viewModel.checkOnDeviceModelFile(localOnDevicePath)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MeditechCyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .emilPressScale(testPathInteraction, pressedScale = 0.96f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Verifica File Pesi su Smartphone",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeditechCyanPrimary
                        )
                    }

                    if (uiState.onDeviceModelStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isFound = uiState.onDeviceModelStatus.contains("verificato", ignoreCase = true) ||
                                uiState.onDeviceModelStatus.contains("pronto", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFound) MeditechTealSoft else MeditechUrgentSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.onDeviceModelStatus,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (isFound) MeditechTealActive else MeditechUrgentRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

            if (uiState.isUpdateAvailable) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MeditechTealSoft),
                    border = BorderStroke(1.2.dp, MeditechTealActive),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MeditechTealActive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aggiornamento v${uiState.updateManifest?.latestVersionName ?: "Disponibile"} Rilevato!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeditechTealActive
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "È disponibile una nuova versione Git (Build #${uiState.updateManifest?.latestVersionCode ?: 4}). Puoi scaricarla e installarla con 1-Tap.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = MeditechTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onDismiss() // Chiude il foglio impostazioni e apre il dialog di aggiornamento 1-Tap
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeditechTealActive),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apri Installazione 1-Tap", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
