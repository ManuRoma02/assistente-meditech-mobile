package com.example.assistentemeditechmobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.theme.*

data class MeditechSlashCommand(
    val command: String,
    val syntax: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val insertText: String = command,
    val isDirectAction: Boolean = false
)

val MeditechSlashCommandsList = listOf(
    MeditechSlashCommand(
        command = "/agenda",
        syntax = "/agenda",
        title = "Agenda & Impegni di Oggi",
        description = "Mostra gli impegni, visite e riunioni cliniche in programma per oggi",
        iconEmoji = "📅",
        insertText = "/agenda",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/fissa",
        syntax = "/fissa [dettagli]",
        title = "Fissa Nuovo Appuntamento",
        description = "Pianifica visita o incontro con rilevamento conflitti (es: /fissa domani alle 14)",
        iconEmoji = "📝",
        insertText = "/fissa ",
        isDirectAction = false
    ),
    MeditechSlashCommand(
        command = "/email",
        syntax = "/email",
        title = "Briefing Email Cliniche",
        description = "Sintesi esecutiva con IA delle comunicazioni e priorità VIP",
        iconEmoji = "📬",
        insertText = "/email",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/progetti",
        syntax = "/progetti",
        title = "Dossier Ricerca MeDiTech",
        description = "Telemetria e stato avanzamento progetti clinici SUPSI ed EOC attivi",
        iconEmoji = "🔬",
        insertText = "/progetti",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/aggiorna",
        syntax = "/aggiorna",
        title = "Verifica Aggiornamenti OTA",
        description = "Controlla la disponibilità di nuove versioni Git e installa con 1-tap",
        iconEmoji = "🔄",
        insertText = "/aggiorna",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/impostazioni",
        syntax = "/impostazioni",
        title = "Impostazioni & Modello IA",
        description = "Configurazione Qwen 3.5 2B on-device, privacy clinica e parametri posta",
        iconEmoji = "⚙️",
        insertText = "/impostazioni",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/pulisci",
        syntax = "/pulisci",
        title = "Ripulisci Cronologia Chat",
        description = "Azzera la cronologia messaggi della sessione corrente senza toccare i dati",
        iconEmoji = "🧹",
        insertText = "/pulisci",
        isDirectAction = true
    ),
    MeditechSlashCommand(
        command = "/aiuto",
        syntax = "/aiuto",
        title = "Guida Comandi Rapidi",
        description = "Elenco completo delle istruzioni slash supportate dal motore locale",
        iconEmoji = "💡",
        insertText = "/aiuto",
        isDirectAction = true
    )
)

@Composable
fun MeditechSlashCommandsPopupCard(
    commands: List<MeditechSlashCommand>,
    typedQuery: String,
    onSelectCommand: (MeditechSlashCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MeditechSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(0.8.dp, MeditechBorderCyan),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 290.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con titolo, query filtrata e pulsante chiudi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text("⚡", fontSize = 14.sp)
                    Text(
                        text = "COMANDI RAPIDI MEDITECH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeditechCyanDark,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (typedQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MeditechCyanSoft
                        ) {
                            Text(
                                text = "/$typedQuery",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MeditechCyanPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Chiudi suggerimenti",
                        tint = MeditechTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.6.dp, color = MeditechBorder)

            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun comando rapido per \"/$typedQuery\".\nProva /agenda, /fissa, /email, /progetti, /aggiorna, /aiuto...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(commands, key = { it.command }) { cmd ->
                        Surface(
                            onClick = { onSelectCommand(cmd) },
                            shape = RoundedCornerShape(12.dp),
                            color = MeditechSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Badge icona
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(MeditechCyanSoft)
                                            .border(0.5.dp, MeditechCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cmd.iconEmoji, fontSize = 17.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = cmd.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MeditechTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MeditechCyanSoft
                                            ) {
                                                Text(
                                                    text = cmd.syntax,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = MeditechCyanDark,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = cmd.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeditechTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
}
