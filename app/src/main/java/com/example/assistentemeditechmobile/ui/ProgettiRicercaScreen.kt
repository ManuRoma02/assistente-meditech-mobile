package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistentemeditechmobile.domain.ClinicalProject
import com.example.assistentemeditechmobile.theme.*

/**
 * Schermata Progetti & Dossier Clinici Istituto MeDiTech SUPSI:
 * - Bento Grid con i 3 pilastri scientifici (P4 Medicine, Telemetria & Wearables, NLP Clinico)
 * - Indicatore di telemetria attiva con battito ECG animato
 * - Card dossier clinici in stile Apple HIG con squircles e badge semantici
 */
@Composable
fun ProgettiRicercaScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ISTITUTO MEDITECH SUPSI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.5.sp
                    ),
                    color = MeditechCyanPrimary
                )
                Text(
                    text = "Ricerca & Dossier Clinici",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp
                    ),
                    color = MeditechTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val addInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddDialog = true
                },
                interactionSource = addInteraction,
                shape = CircleShape,
                color = MeditechCyanPrimary,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(40.dp)
                    .cartoonPressSquash(addInteraction, pressedScaleY = 0.90f, squashScaleX = 1.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuovo Progetto",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Bento Grid Pilastri di Ricerca SUPSI
            item {
                ResearchBentoGrid()
            }

            // 2. Intestazione Sezione Dossier
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DOSSIER CLINICI ATTIVI (${uiState.projects.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MeditechCyanDark
                    )
                    Text(
                        text = "Prof. Stéphane Meystre",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MeditechTextTertiary
                    )
                }
            }

            // 3. Elenco Card Dossier
            items(uiState.projects, key = { it.id }) { project ->
                ClinicalProjectCard(project = project)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuovo Dossier Clinico MeDiTech",
                        color = MeditechTextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Titolo Progetto / Sperimentazione") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Codice Studio (es. MDT-2026-X)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Obiettivi Clinici & Dataset") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.viewModelScopeLaunchProject(newTitle, newCode, newDesc)
                            newTitle = ""
                            newCode = ""
                            newDesc = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Crea Dossier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annulla", color = MeditechTextTertiary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Bento Grid dimostrativo dei 3 pilastri tecnologici di Istituto MeDiTech SUPSI.
 */
@Composable
private fun ResearchBentoGrid() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Box Grande: P4 Medicine & Digital Health Telemetry
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MeditechSurface,
            border = BorderStroke(0.8.dp, MeditechBorderCyan),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MeditechTealActive)
                                .graphicsLayer { alpha = pulseAlpha }
                        )
                        Text(
                            text = "TELEMETRIA EDGE ATTIVA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MeditechTealActive
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MeditechTealSoft
                    ) {
                        Text(
                            text = "P4 Medicine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MeditechTealActive,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Piattaforma Sensori Wearable & Telemetria Clinica",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MeditechTextPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Flussi biometrici in tempo reale (ECG, SpO2, fotopletismografia) con elaborazione sul dispositivo e predizione anomalie senza esportazione dati.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = MeditechTextSecondary
                )
            }
        }

        // 2 Box Affiancati: NLP Clinico & Privacy Svizzera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MeditechSurface,
                border = BorderStroke(0.8.dp, MeditechBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "NLP Clinico",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                    Text(
                        text = "Qwen 3.5 on-device per sintesi cartelle e appuntamenti.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = MeditechTextTertiary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MeditechSurface,
                border = BorderStroke(0.8.dp, MeditechBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MeditechCyanDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Privacy Svizzera",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                    Text(
                        text = "Dati confinati all'interno del campus SUPSI Viganello.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = MeditechTextTertiary
                    )
                }
            }
        }
    }
}

private fun MeditechViewModel.viewModelScopeLaunchProject(title: String, code: String, desc: String) {
    val vm = this
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).run {
        vm.loadInitialData()
    }
}

/**
 * Card Dossier Clinico in stile Apple HIG.
 */
@Composable
fun ClinicalProjectCard(project: ClinicalProject) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeditechSurface,
        border = BorderStroke(0.8.dp, MeditechBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MeditechCyanSoft
                ) {
                    Text(
                        text = project.codice,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = MeditechCyanDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MeditechTealSoft
                ) {
                    Text(
                        text = project.stato,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MeditechTealActive,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.titolo,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 21.sp
                ),
                color = MeditechTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.descrizione,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MeditechTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = project.responsabileScientifico,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MeditechTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MeditechTextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${project.documentCount} doc",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTextTertiary
                    )
                }
            }
        }
    }
}
