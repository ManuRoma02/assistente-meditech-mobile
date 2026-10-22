package com.example.assistentemeditechmobile.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.ChatMessage
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Formattatore Markdown Nativo per MeDiTech AI:
 * Converte la sintassi **grassetto**, *corsivo*, `codice` e [link](url)
 * in un vero AnnotatedString tipografico, rimuovendo completamente
 * i simboli asterisco e markdown raw dalla schermata.
 */
fun formatMeditechMessageText(raw: String, isUser: Boolean): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val n = raw.length
        while (i < n) {
            // **grassetto**
            if (i + 1 < n && raw[i] == '*' && raw[i + 1] == '*') {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color.White else MeditechTextPrimary
                        )
                    ) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // *corsivo*
            if (raw[i] == '*') {
                val end = raw.indexOf('*', i + 1)
                if (end != -1 && end > i + 1 && !raw.substring(i + 1, end).contains('\n')) {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isUser) Color.White.copy(alpha = 0.9f) else MeditechTextSecondary
                        )
                    ) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // `codice inline`
            if (raw[i] == '`') {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUser) Color(0xFFE0F7FA) else MeditechCyanDark
                        )
                    ) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // [label link](url)
            if (raw[i] == '[') {
                val labelEnd = raw.indexOf(']', i + 1)
                if (labelEnd != -1 && labelEnd + 1 < n && raw[labelEnd + 1] == '(') {
                    val urlEnd = raw.indexOf(')', labelEnd + 2)
                    if (urlEnd != -1) {
                        val label = raw.substring(i + 1, labelEnd)
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) Color.White else MeditechCyanPrimary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(label)
                        }
                        i = urlEnd + 1
                        continue
                    }
                }
            }
            append(raw[i])
            i++
        }
    }
}

@Composable
fun MeditechAiChatScreen(
    viewModel: MeditechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scorrimento automatico fluido al nuovo messaggio in arrivo
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeditechBg)
            .padding(horizontal = 16.dp)
    ) {
        // Feed Messaggi Chat con SelectionContainer & Animazione Poof Cartoonesca
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
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
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MeditechCyanPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${uiState.selectedAiModel} sta elaborando in locale on-device...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MeditechCyanDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bubble conversazionale ad altissima leggibilità e formattazione markdown reale:
 * - Wrap in SelectionContainer per selezione testo nativa
 * - Dimensione carattere 15.5sp con altezza riga confortevole (22sp)
 * - Tasti rapidi Copia (con check icon feedback & rimbalzo cartoonesco) e Condividi
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
                modifier = Modifier.fillMaxWidth(0.86f)
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
                                    text = "MeDiTech AI • SUPSI (Locale)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MeditechCyanDark
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // SelectionContainer per selezione testo nativa con Markdown formattato
                        SelectionContainer {
                            Text(
                                text = formatMeditechMessageText(message.text, isUser),
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
                    .cartoonPressSquash(copyInteraction, pressedScaleY = 0.90f, squashScaleX = 1.06f)
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
                    .cartoonPressSquash(shareInteraction, pressedScaleY = 0.90f, squashScaleX = 1.06f)
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
