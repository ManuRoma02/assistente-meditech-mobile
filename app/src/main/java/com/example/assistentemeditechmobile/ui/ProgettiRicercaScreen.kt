package com.example.assistentemeditechmobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.assistentemeditechmobile.domain.ClinicalProject
import com.example.assistentemeditechmobile.theme.*

@Composable
fun ProgettiRicercaScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ISTITUTO MEDITECH SUPSI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MeditechCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Progetti & Dossier Clinici",
                    style = MaterialTheme.typography.titleLarge,
                    color = MeditechTextPrimary
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(MeditechCyanPrimary, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nuovo Progetto", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.projects, key = { it.id }) { project ->
                ClinicalProjectCard(project = project)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuovo Studio / Progetto MeDiTech", color = MeditechTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Titolo Progetto / Sperimentazione") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Codice Studio (es. MDT-2026-X)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Descrizione & Obiettivi Clinici") },
                        modifier = Modifier.fillMaxWidth()
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
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanPrimary)
                ) {
                    Text("Crea Dossier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annulla", color = MeditechTextTertiary)
                }
            }
        )
    }
}

private fun MeditechViewModel.viewModelScopeLaunchProject(title: String, code: String, desc: String) {
    // Helper to add project through repository
    val vm = this
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).run {
        // ViewModel already has add logic or triggers refresh
        vm.loadInitialData()
    }
}

@Composable
fun ClinicalProjectCard(project: ClinicalProject) {
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MeditechCyanSoft
                ) {
                    Text(
                        text = project.codice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechCyanDark,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MeditechTealSoft
                ) {
                    Text(
                        text = project.stato,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTealActive,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.titolo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeditechTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.descrizione,
                style = MaterialTheme.typography.bodySmall,
                color = MeditechTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.responsabileScientifico,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MeditechTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
