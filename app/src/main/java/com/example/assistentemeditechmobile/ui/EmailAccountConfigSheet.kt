package com.example.assistentemeditechmobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.EmailAccountConfig
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAccountConfigSheet(
    config: EmailAccountConfig,
    onSaveConfig: (EmailAccountConfig) -> Unit,
    onTestConnection: suspend (EmailAccountConfig) -> Result<String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(config.provider) }
    var emailAddress by remember { mutableStateOf(config.emailAddress) }
    var displayName by remember { mutableStateOf(config.displayName) }
    var password by remember { mutableStateOf("••••••••••••") }
    var showPassword by remember { mutableStateOf(false) }
    var imapHost by remember { mutableStateOf(config.imapHost) }
    var imapPort by remember { mutableStateOf(config.imapPort.toString()) }
    var smtpHost by remember { mutableStateOf(config.smtpHost) }
    var smtpPort by remember { mutableStateOf(config.smtpPort.toString()) }
    var autoAiRecap by remember { mutableStateOf(config.autoAiRecap) }
    var vipFilterEnabled by remember { mutableStateOf(config.vipFilterEnabled) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }

    fun applyProviderPreset(provider: String) {
        selectedProvider = provider
        when (provider) {
            "SUPSI / EOC" -> {
                imapHost = "mail.supsi.ch"
                imapPort = "993"
                smtpHost = "smtp.supsi.ch"
                smtpPort = "587"
                if (!emailAddress.endsWith("@supsi.ch") && !emailAddress.endsWith("@eoc.ch")) {
                    emailAddress = "utente@supsi.ch"
                }
            }
            "Outlook 365" -> {
                imapHost = "outlook.office365.com"
                imapPort = "993"
                smtpHost = "smtp.office365.com"
                smtpPort = "587"
            }
            "Gmail" -> {
                imapHost = "imap.gmail.com"
                imapPort = "993"
                smtpHost = "smtp.gmail.com"
                smtpPort = "587"
            }
            "IMAP Altro" -> {
                imapHost = "imap.server.ch"
                imapPort = "993"
                smtpHost = "smtp.server.ch"
                smtpPort = "587"
            }
        }
        testResultText = null
        testResultSuccess = null
    }

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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Configurazione Email",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                    Text(
                        text = "Collega la tua casella SUPSI, EOC o ospedaliera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary
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

            Spacer(modifier = Modifier.height(18.dp))

            // Apple Style Segmented Provider Selector
            Text(
                text = "PROVIDER PREDEFINITO (1-TAP SETUP)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MeditechGroupedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("SUPSI / EOC", "Outlook 365", "Gmail", "IMAP Altro").forEach { providerName ->
                        val isSelected = selectedProvider == providerName
                        val interaction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = { applyProviderPreset(providerName) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MeditechSurface else Color.Transparent,
                            shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .emilPressScale(interaction, pressedScale = 0.95f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = providerName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MeditechCyanDark else MeditechTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Grouped Inset Card: Account & Server Details
            Text(
                text = "DETTAGLI ACCOUNT & CONNESSIONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechSurface),
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Indirizzo Email
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it },
                        label = { Text("Indirizzo Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MeditechCyanPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nome Visualizzato
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nome Visualizzato / Firma") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MeditechCyanPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password / Token
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password o Token App") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MeditechCyanPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MeditechTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MeditechBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Host IMAP e Porta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = imapHost,
                            onValueChange = { imapHost = it },
                            label = { Text("Host IMAP") },
                            singleLine = true,
                            modifier = Modifier.weight(2.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = imapPort,
                            onValueChange = { imapPort = it },
                            label = { Text("Porta") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Host SMTP e Porta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = smtpHost,
                            onValueChange = { smtpHost = it },
                            label = { Text("Host SMTP") },
                            singleLine = true,
                            modifier = Modifier.weight(2.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = smtpPort,
                            onValueChange = { smtpPort = it },
                            label = { Text("Porta") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Grouped Inset Card: Apple Intelligence Triage & Recaps
            Text(
                text = "AUTOMAZIONI & RECAP INTELLIGENTE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeditechCyanDark,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MeditechSurface),
                border = BorderStroke(1.dp, MeditechBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recap AI Automatico in Arrivo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MeditechTextPrimary
                            )
                            Text(
                                text = "Genera istantaneamente la sintesi e le azioni richieste con Qwen 3.5 on-device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = autoAiRecap,
                            onCheckedChange = { autoAiRecap = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MeditechCyanPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MeditechBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filtro Prioritario VIP Clinico",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MeditechTextPrimary
                            )
                            Text(
                                text = "Riconosci comunicazioni da Prof. Meystre, EOC Ticino e Cardiocentro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeditechTextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = vipFilterEnabled,
                            onCheckedChange = { vipFilterEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MeditechCyanPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Test Connection Feedback Box
            if (testResultText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (testResultSuccess == true) MeditechCyanUltraLight else MeditechWarningSoft,
                    border = BorderStroke(1.dp, if (testResultSuccess == true) MeditechCyanPrimary else MeditechWarningAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testResultSuccess == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (testResultSuccess == true) MeditechCyanPrimary else MeditechWarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = testResultText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MeditechTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Test Connection Button (Apple Outlined Style)
            val testInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    testResultText = null
                    testResultSuccess = null
                    coroutineScope.launch {
                        val tempConfig = config.copy(
                            emailAddress = emailAddress,
                            displayName = displayName,
                            provider = selectedProvider,
                            imapHost = imapHost,
                            imapPort = imapPort.toIntOrNull() ?: 993,
                            smtpHost = smtpHost,
                            smtpPort = smtpPort.toIntOrNull() ?: 587,
                            autoAiRecap = autoAiRecap,
                            vipFilterEnabled = vipFilterEnabled
                        )
                        val res = onTestConnection(tempConfig)
                        isTestingConnection = false
                        if (res.isSuccess) {
                            testResultSuccess = true
                            testResultText = res.getOrNull()
                        } else {
                            testResultSuccess = false
                            testResultText = res.exceptionOrNull()?.message ?: "Errore di connessione"
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MeditechCyanPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MeditechCyanPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .emilPressScale(testInteraction)
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MeditechCyanPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifica crittografia SSL/TLS in corso...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifica Parametri (Test IMAP/SMTP)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Configuration Button (Apple Filled Prominent Style)
            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    val updated = config.copy(
                        emailAddress = emailAddress.trim(),
                        displayName = displayName.trim(),
                        provider = selectedProvider,
                        imapHost = imapHost.trim(),
                        imapPort = imapPort.toIntOrNull() ?: 993,
                        smtpHost = smtpHost.trim(),
                        smtpPort = smtpPort.toIntOrNull() ?: 587,
                        autoAiRecap = autoAiRecap,
                        vipFilterEnabled = vipFilterEnabled,
                        isConnected = true,
                        lastSyncTimeStr = "Ora"
                    )
                    onSaveConfig(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .emilPressScale(saveInteraction)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salva Configurazione",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
