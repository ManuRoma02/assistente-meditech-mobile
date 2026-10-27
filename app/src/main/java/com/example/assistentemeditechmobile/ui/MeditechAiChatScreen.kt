package com.example.assistentemeditechmobile.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.ChatMessage
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata Chat AI MeDiTech SUPSI completamente ridisegnata:
 * - Rendering gerarchico Markdown / Block-level (titoli, liste a punti indentate, callout clinici, codici)
 * - Generative UI: Card interattive per verifica appuntamenti, Google Meet 1-tap, Google Maps
 * - Bolle stile Apple HIG con squircles morbidi (22.dp) e gradienti clinici ad alto contrasto
 * - Micro-interazioni fisiche Emil Kowalski con feedback aptico immediato
 * - Chip di suggerimento rapido per task clinici MeDiTech
 */
@Composable
fun MeditechAiChatScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    // Scorrimento fluido all'ultimo messaggio
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 14.dp)
    ) {
        // Banner Easter Egg Witty / Sarcastico (animazione cartoonesca)
        AnimatedVisibility(
            visible = uiState.easterEggMessage != null,
            enter = fadeIn(tween(240)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            uiState.easterEggMessage?.let { message ->
                Surface(
                    color = MeditechCyanSoft.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.6.dp, MeditechCyanPrimary.copy(alpha = 0.35f)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            ),
                            color = MeditechCyanDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::dismissEasterEgg,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi battuta",
                                tint = MeditechCyanDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Feed Messaggi Chat
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp)
        ) {
            // Stato vuoto illustrato se non ci sono messaggi
            if (uiState.chatMessages.isEmpty()) {
                item {
                    ChatEmptyWelcomeState(
                        onSelectSuggestion = { suggestion ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendUserChatMessage(suggestion)
                        }
                    )
                }
            }

            itemsIndexed(uiState.chatMessages, key = { _, message -> message.id }) { index, message ->
                ChatBubble(
                    message = message,
                    index = index,
                    isDissolving = uiState.isClearingChat
                )
            }

            // Indicatore di elaborazione on-device dell'IA locale
            if (uiState.isAiProcessing) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(start = 6.dp, top = 4.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeditechSurface)
                            .border(0.6.dp, MeditechBorderCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MeditechCyanPrimary,
                            strokeWidth = 2.2.dp
                        )
                        Column {
                            Text(
                                text = "Elaborazione Locale On-Device",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MeditechCyanDark
                            )
                            Text(
                                text = "${uiState.selectedAiModel} • Nessun dato inviato al cloud",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MeditechTextTertiary
                            )
                        }
                    }
                }
            }
        }

        // Striscia Orizzontale Suggerimenti Rapidi MeDiTech (sopra la barra di digitazione)
        val suggestions = remember {
            listOf(
                "📅 /agenda",
                "📝 /fissa incontro con Stéphane Meystre",
                "📬 /email",
                "🔬 /progetti",
                "🔄 /aggiorna"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { chipText ->
                val chipInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.sendUserChatMessage(chipText.substringAfter(" ").trim())
                    },
                    interactionSource = chipInteraction,
                    shape = RoundedCornerShape(20.dp),
                    color = MeditechSurface,
                    border = BorderStroke(0.8.dp, MeditechBorderCyan),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.cartoonPressSquash(chipInteraction, pressedScaleY = 0.92f)
                ) {
                    Text(
                        text = chipText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = MeditechCyanDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stato di benvenuto clinico quando la chat è vuota o appena ripulita.
 */
@Composable
private fun ChatEmptyWelcomeState(onSelectSuggestion: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MeditechSurface,
        border = BorderStroke(0.8.dp, MeditechBorderCyan),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(MeditechCyanPrimary, MeditechCyanDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Assistente Clinico MeDiTech",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MeditechTextPrimary
            )

            Text(
                text = "Motore linguistico on-device SUPSI. Gestione agenda visite, resoconto email dei ricercatori e dossier clinici nel pieno rispetto della privacy svizzera.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = MeditechTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            HorizontalDivider(
                color = MeditechBorder.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Azioni consigliate con 1 tap:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MeditechTextTertiary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSelectSuggestion("Fissa incontro di ricerca clinica con Stéphane Meystre") },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechCyanSoft),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🗓 Fissa Meeting",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MeditechCyanDark
                    )
                }

                Button(
                    onClick = { onSelectSuggestion("Sintetizza email importanti") },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechGroupedBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "📬 Sintesi Email",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MeditechTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Bolla conversazionale ad altissima leggibilità e design Apple HIG:
 * - Parsing e rendering gerarchico Markdown (titoli, liste indentate, callout, codice)
 * - Card Generative UI integrate per appuntamenti e meeting video
 * - Squircle morbidi (22.dp) con gradienti clinici
 * - Tasti Copia (con checkmark animato) e Condividi
 * - Animazione fisica di entrata (Emil Kowalski spring) e poof dissolve
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    index: Int = 0,
    isDissolving: Boolean = false
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var isCopied by remember { mutableStateOf(false) }

    // Animazione di ingresso elastica Emil Kowalski
    val entranceProgress = remember { Animatable(0.92f) }
    LaunchedEffect(message.id) {
        if (ValueAnimator.areAnimatorsEnabled()) {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            entranceProgress.snapTo(1f)
        }
    }

    val timeFormatted = remember(message.timestampMillis) {
        SimpleDateFormat("HH:mm", Locale.ITALIAN).format(Date(message.timestampMillis))
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxBubbleWidth = remember(screenWidthDp) {
        (screenWidthDp * 0.88f).coerceIn(260.dp, 580.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cartoonPoofDissolve(isDissolving = isDissolving, index = index)
            .graphicsLayer {
                scaleX = entranceProgress.value
                scaleY = entranceProgress.value
                alpha = ((entranceProgress.value - 0.80f) / 0.20f).coerceIn(0f, 1f)
                translationY = (1f - entranceProgress.value) * 35f
            },
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MeditechCyanPrimary, MeditechCyanDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (isUser) 22.dp else 6.dp,
                    bottomEnd = if (isUser) 6.dp else 22.dp
                ),
                color = if (isUser) MeditechCyanPrimary else MeditechSurface,
                border = if (!isUser) BorderStroke(0.8.dp, MeditechBorder) else null,
                shadowElevation = if (isUser) 2.dp else 1.dp,
                modifier = Modifier
                    .widthIn(min = 90.dp, max = maxBubbleWidth)
                    .background(
                        if (isUser) {
                            Brush.linearGradient(
                                listOf(MeditechCyanPrimary, MeditechCyanDark)
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.White, Color.White))
                        },
                        shape = RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp,
                            bottomStart = if (isUser) 22.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 22.dp
                        )
                    )
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    if (!isUser) {
                        // Riga di accento clinico verticale a sinistra
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.5.dp)
                                .background(MeditechCyanPrimary)
                        )
                    }

                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header interno bolla assistente
                        if (!isUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "MeDiTech AI",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MeditechCyanDark
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MeditechCyanSoft
                                    ) {
                                        Text(
                                            text = "SUPSI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MeditechCyanDark,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MeditechTealSoft
                                ) {
                                    Text(
                                        text = "Qwen Locale",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MeditechTealActive,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Testo strutturato con Markdown / Block-level ad alta gerarchia
                        StructuredMessageContent(
                            text = message.text,
                            isUser = isUser
                        )

                        // Generative UI: Card Interattiva di Verifica Appuntamento (se creato)
                        message.createdEvent?.let { event ->
                            Spacer(Modifier.height(10.dp))
                            ClinicalAppointmentVerificationCard(event = event)
                        }

                        // Timestamp in basso a destra
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isUser) Color.White.copy(alpha = 0.82f) else MeditechTextTertiary
                            )
                        }
                    }
                }
            }
        }

        // Tasti Rapidi Azioni: Copia & Condividi
        Row(
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    start = if (isUser) 0.dp else 42.dp,
                    end = if (isUser) 4.dp else 0.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Tasto Copia con feedback visivo e aptico immediato
            val copyInteraction = remember { MutableInteractionSource() }
            val copyScale by animateFloatAsState(
                targetValue = if (isCopied) 1.15f else 1f,
                animationSpec = MeditechClinicalSpring,
                label = "copyScale"
            )
            val copyBgColor by animateColorAsState(
                targetValue = if (isCopied) MeditechTealSoft else MeditechGroupedBg,
                animationSpec = spring(),
                label = "copyBgColor"
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = copyBgColor,
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(message.text))
                        isCopied = true
                        Toast.makeText(context, "Messaggio copiato negli appunti!", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            delay(1800)
                            isCopied = false
                        }
                    }
                    .cartoonPressSquash(copyInteraction, pressedScaleY = 0.90f, squashScaleX = 1.06f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copia",
                        tint = if (isCopied) MeditechTealActive else MeditechTextTertiary,
                        modifier = Modifier
                            .size(12.dp)
                            .graphicsLayer {
                                scaleX = copyScale
                                scaleY = copyScale
                            }
                    )
                    Text(
                        text = if (isCopied) "Copiato!" else "Copia",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isCopied) MeditechTealActive else MeditechTextSecondary,
                        fontWeight = if (isCopied) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // 2. Tasto Condividi
            val shareInteraction = remember { MutableInteractionSource() }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MeditechGroupedBg,
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, message.text)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Condividi messaggio via:")
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                    }
                    .cartoonPressSquash(shareInteraction, pressedScaleY = 0.90f, squashScaleX = 1.06f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Condividi",
                        tint = MeditechTextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Condividi",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MeditechTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
