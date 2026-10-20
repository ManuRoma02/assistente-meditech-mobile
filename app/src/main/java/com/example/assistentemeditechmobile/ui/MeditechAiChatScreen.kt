package com.example.assistentemeditechmobile.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.ChatMessage
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MeditechAiChatScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    // Speech Recognizer Launcher for Voice Commands
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                viewModel.sendUserChatMessage(spokenText)
                coroutineScope.launch {
                    listState.animateScrollToItem(uiState.chatMessages.size)
                }
            }
        }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MeditechUrgentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ripulisci Cronologia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeditechTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Vuoi dissolvere e ripulire tutti i messaggi della sessione corrente? I dati dell'agenda e le email non verranno toccati.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeditechTextSecondary
                )
            },
            confirmButton = {
                val delInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearChatHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeditechUrgentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.emilPressScale(delInteraction, pressedScale = 0.90f)
                ) {
                    Text("Dissolvi e Pulisci", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Annulla", color = MeditechTextSecondary)
                }
            },
            containerColor = MeditechSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 16.dp)
    ) {
        // Chat Messages Feed with SelectionContainer & Animated Entrance / Cartoon Poof
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            itemsIndexed(uiState.chatMessages, key = { _, message -> message.id }) { index, message ->
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = CartoonyHighBouncySpring,
                        initialScale = 0.88f
                    ) + fadeIn(tween(220))
                ) {
                    ChatBubble(
                        message = message,
                        index = index,
                        isDissolving = uiState.isClearingChat
                    )
                }
            }

            if (uiState.isAiProcessing) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MeditechCyanPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${uiState.selectedAiModel} sta elaborando la richiesta...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MeditechCyanDark
                        )
                    }
                }
            }
        }

        // Quick Suggestion Pills & Clear Chat Trigger with Emil Kowalski spring feedback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.chatMessages.size > 1) {
                val clearInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { showClearConfirmDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MeditechSurface,
                    border = BorderStroke(1.dp, MeditechUrgentRed.copy(alpha = 0.4f)),
                    modifier = Modifier.emilPressScale(clearInteraction, pressedScale = 0.90f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Pulisci chat",
                            tint = MeditechUrgentRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pulisci",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeditechUrgentRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            listOf(
                "Fissa meeting 30m con Stéphane Meystre",
                "Cosa ho in agenda oggi?",
                "Pianifica call online domani alle 15",
                "Riunione con EOC Ticino"
            ).forEach { prompt ->
                val pillInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        viewModel.sendUserChatMessage(prompt)
                        coroutineScope.launch {
                            listState.animateScrollToItem(uiState.chatMessages.size)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MeditechSurface,
                    border = BorderStroke(1.dp, MeditechBorderCyan),
                    modifier = Modifier.emilPressScale(pillInteraction, pressedScale = 0.93f)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechCyanDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Bar: Text Field + Voice Mic + Send Button
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MeditechSurface,
            border = BorderStroke(1.dp, MeditechBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Dictation Button with Spring Animation
                val micInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla ora... (es: 'Fissa appuntamento domani alle 14')")
                        }
                        speechLauncher.launch(intent)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(MeditechCyanSoft, CircleShape)
                        .cartoonAcousticPulse(uiState.isVoiceListening)
                        .emilPressScale(micInteraction, pressedScale = 0.88f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Comando Vocale",
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Scrivi o detta un comando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MeditechTextTertiary
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                val sendInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendUserChatMessage(inputText)
                            inputText = ""
                            coroutineScope.launch {
                                listState.animateScrollToItem(uiState.chatMessages.size)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (inputText.isNotBlank()) MeditechCyanPrimary else MeditechGroupedBg,
                            CircleShape
                        )
                        .emilPressScale(sendInteraction)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Invia",
                        tint = if (inputText.isNotBlank()) Color.White else MeditechTextTertiary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bubble conversazionale ad altissima leggibilità:
 * - Wrap in SelectionContainer per selezione testo nativa
 * - Dimensione carattere aumentata a 15.5sp con altezza riga confortevole (22sp)
 * - Tasti rapidi Copia (con check icon feedback) e Condividi (chooser Android nativo)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cartoonPoofDissolve(isDissolving = isDissolving, index = index),
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
                        .background(MeditechCyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) MeditechCyanPrimary else MeditechSurface,
                border = if (!isUser) BorderStroke(1.dp, MeditechBorder) else null,
                shadowElevation = if (isUser) 2.dp else 1.dp,
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.5.dp)
                                .background(MeditechCyanPrimary)
                        )
                    }

                    Column(modifier = Modifier.padding(14.dp)) {
                        if (!isUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MeDiTech AI • SUPSI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MeditechCyanDark
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // SelectionContainer per selezione testo nativa
                        SelectionContainer {
                            Text(
                                text = message.text,
                                style = TextStyle(
                                    fontSize = 15.5.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isUser) Color.White else MeditechTextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Tasti Rapidi Azioni: Copia & Condividi
        Row(
            modifier = Modifier
                .padding(top = 4.dp, start = if (isUser) 0.dp else 42.dp, end = if (isUser) 4.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Tasto Copia con feedback a rimbalzo cartoonesco
            val copyInteraction = remember { MutableInteractionSource() }
            val copyScale by animateFloatAsState(
                targetValue = if (isCopied) 1.25f else 1f,
                animationSpec = CartoonyHighBouncySpring,
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
                    .emilPressScale(copyInteraction, pressedScale = 0.88f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                        context.startActivity(shareIntent)
                    }
                    .emilPressScale(shareInteraction, pressedScale = 0.90f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
