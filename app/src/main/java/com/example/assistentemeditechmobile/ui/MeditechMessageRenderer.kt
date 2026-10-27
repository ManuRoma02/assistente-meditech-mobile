package com.example.assistentemeditechmobile.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.domain.CalendarEventItem
import com.example.assistentemeditechmobile.theme.*

// =========================================================================
// ME-DITECH CLINICAL MARKDOWN & STRUCTURED GENERATIVE UI RENDERER
// Transforms raw asterisks and flat paragraphs into Apple HIG executive blocks,
// crisp typography, indented clinical bullet lists, and interactive cards.
// =========================================================================

sealed class MessageBlock {
    data class Header(val level: Int, val text: String) : MessageBlock()
    data class BulletItem(val bullet: String, val text: String) : MessageBlock()
    data class NumberedItem(val number: String, val text: String) : MessageBlock()
    data class Callout(val isWarning: Boolean, val title: String?, val text: String) : MessageBlock()
    object Divider : MessageBlock()
    data class Paragraph(val text: String) : MessageBlock()
}

/**
 * Analizza il testo raw multilinea prodotto dal motore IA o dall'utente
 * suddividendolo in blocchi semantici strutturati ad alta leggibilità.
 */
fun parseMessageBlocks(raw: String): List<MessageBlock> {
    val lines = raw.lines()
    val blocks = mutableListOf<MessageBlock>()
    var currentParagraph = StringBuilder()

    fun flushParagraph() {
        if (currentParagraph.isNotEmpty()) {
            val pText = currentParagraph.toString().trim()
            if (pText.isNotEmpty()) {
                blocks.add(MessageBlock.Paragraph(pText))
            }
            currentParagraph = StringBuilder()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        // 1. Linea Divisoria
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            flushParagraph()
            blocks.add(MessageBlock.Divider)
            continue
        }

        // 2. Intestazioni (#, ##, ###)
        if (trimmed.startsWith("### ")) {
            flushParagraph()
            blocks.add(MessageBlock.Header(level = 3, text = trimmed.removePrefix("### ").trim()))
            continue
        }
        if (trimmed.startsWith("## ")) {
            flushParagraph()
            blocks.add(MessageBlock.Header(level = 2, text = trimmed.removePrefix("## ").trim()))
            continue
        }
        if (trimmed.startsWith("# ")) {
            flushParagraph()
            blocks.add(MessageBlock.Header(level = 1, text = trimmed.removePrefix("# ").trim()))
            continue
        }

        // 3. Callout / Citazioni / Conflitti (> o [Attenzione Conflitto]: o ⚠️)
        if (trimmed.startsWith("> ")) {
            flushParagraph()
            val content = trimmed.removePrefix("> ").trim()
            val isWarning = content.contains("conflitto", ignoreCase = true) ||
                    content.contains("attenzione", ignoreCase = true) ||
                    content.contains("urgente", ignoreCase = true)
            blocks.add(MessageBlock.Callout(isWarning = isWarning, title = null, text = content))
            continue
        }
        if (trimmed.startsWith("[Attenzione Conflitto]:") || trimmed.startsWith("⚠️")) {
            flushParagraph()
            val content = trimmed.removePrefix("[Attenzione Conflitto]:").removePrefix("⚠️").trim()
            blocks.add(MessageBlock.Callout(isWarning = true, title = "Conflitto d'Agenda Rilevato", text = content))
            continue
        }

        // 4. Punti Elenco Numerati (1. , 2. )
        val numberedMatch = Regex("""^(\d+)[\.\)]\s+(.*)$""").find(trimmed)
        if (numberedMatch != null) {
            flushParagraph()
            val num = numberedMatch.groupValues[1]
            val rest = numberedMatch.groupValues[2]
            blocks.add(MessageBlock.NumberedItem(number = num, text = rest))
            continue
        }

        // 5. Punti Elenco a pallino (•, -, *, +)
        if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            flushParagraph()
            val bulletChar = trimmed.take(2)
            val rest = trimmed.substring(2).trim()
            blocks.add(MessageBlock.BulletItem(bullet = bulletChar, text = rest))
            continue
        }

        // Altrimenti accumula nel paragrafo
        if (currentParagraph.isNotEmpty()) {
            currentParagraph.append("\n")
        }
        currentParagraph.append(trimmed)
    }

    flushParagraph()
    return blocks
}

fun formatMeditechMessageText(raw: String, isUser: Boolean = false): AnnotatedString = formatInlineMarkdown(raw, isUser)

/**
 * Converte la formattazione inline Markdown (**grassetto**, *corsivo*, `codice`, [link](url))
 * in un AnnotatedString tipografico privo di simboli raw e con contrasto calibrato.
 */
fun formatInlineMarkdown(raw: String, isUser: Boolean): AnnotatedString {
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
                            color = if (isUser) Color.White.copy(alpha = 0.92f) else MeditechTextSecondary
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
                            color = if (isUser) Color(0xFFE0F2FE) else MeditechCyanDark,
                            background = if (isUser) Color.White.copy(alpha = 0.18f) else MeditechCyanSoft.copy(alpha = 0.6f)
                        )
                    ) {
                        append(" " + raw.substring(i + 1, end) + " ")
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

/**
 * Rendering Composable di un messaggio completo con tipografia gerarchica,
 * spaziature calibrate e formattazione clinica distinta per ogni blocco.
 */
@Composable
fun StructuredMessageContent(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMessageBlocks(text) }

    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MessageBlock.Header -> {
                        val (fontSize, topPad) = when (block.level) {
                            1 -> Pair(18.sp, 6.dp)
                            2 -> Pair(16.5.sp, 4.dp)
                            else -> Pair(15.sp, 3.dp)
                        }
                        Text(
                            text = formatInlineMarkdown(block.text, isUser),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                lineHeight = (fontSize.value + 6).sp
                            ),
                            color = if (isUser) Color.White else MeditechCyanDark,
                            modifier = Modifier.padding(top = topPad)
                        )
                    }

                    is MessageBlock.BulletItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(5.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isUser) Color.White.copy(alpha = 0.85f) else MeditechCyanPrimary)
                            )
                            Text(
                                text = formatInlineMarkdown(block.text, isUser),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 21.sp,
                                    letterSpacing = (-0.15).sp
                                ),
                                color = if (isUser) Color.White else MeditechTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MessageBlock.NumberedItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isUser) Color.White.copy(alpha = 0.22f) else MeditechCyanSoft,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = block.number,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isUser) Color.White else MeditechCyanDark
                                    )
                                }
                            }
                            Text(
                                text = formatInlineMarkdown(block.text, isUser),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 21.sp,
                                    letterSpacing = (-0.15).sp
                                ),
                                color = if (isUser) Color.White else MeditechTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MessageBlock.Callout -> {
                        val bgColor = if (block.isWarning) MeditechWarningSoft else MeditechCyanSoft.copy(alpha = 0.65f)
                        val borderColor = if (block.isWarning) MeditechWarningAmber.copy(alpha = 0.5f) else MeditechCyanPrimary.copy(alpha = 0.35f)
                        val iconColor = if (block.isWarning) MeditechWarningAmber else MeditechCyanDark
                        val iconVector = if (block.isWarning) Icons.Default.WarningAmber else Icons.Default.Info

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUser) Color.White.copy(alpha = 0.16f) else bgColor,
                            border = BorderStroke(0.8.dp, if (isUser) Color.White.copy(alpha = 0.3f) else borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isUser) Color.White else iconColor,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .padding(top = 1.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    if (block.title != null) {
                                        Text(
                                            text = block.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isUser) Color.White else iconColor
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = formatInlineMarkdown(block.text, isUser),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = if (isUser) Color.White else MeditechTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    is MessageBlock.Divider -> {
                        HorizontalDivider(
                            color = if (isUser) Color.White.copy(alpha = 0.25f) else MeditechBorder,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }

                    is MessageBlock.Paragraph -> {
                        Text(
                            text = formatInlineMarkdown(block.text, isUser),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                letterSpacing = (-0.15).sp
                            ),
                            color = if (isUser) Color.White else MeditechTextPrimary
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// GENERATIVE UI: CARD APPUNTAMENTO CLINICO CONFERMATO (INLINE / SOTTO BOLLA)
// =========================================================================

@Composable
fun ClinicalAppointmentVerificationCard(
    event: CalendarEventItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val cardScale = remember { Animatable(0.92f) }
    LaunchedEffect(event.id) {
        if (ValueAnimator.areAnimatorsEnabled()) {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            cardScale.snapTo(1f)
        }
    }

    val startH = if (event.startIso.length >= 16) event.startIso.substring(11, 16) else "14:30"
    val endH = if (event.endIso.length >= 16) event.endIso.substring(11, 16) else "15:30"
    val dateStr = if (event.startIso.length >= 10) event.startIso.substring(0, 10) else "Oggi"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MeditechSurface,
        border = BorderStroke(1.dp, MeditechBorderCyan),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badge di verifica clinica in alto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MeditechTealSoft,
                    border = BorderStroke(0.6.dp, MeditechTealActive.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MeditechTealActive,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "REGISTRATO IN AGENDA SUPSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = MeditechTealActive
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MeditechCyanSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MeditechCyanDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$startH - $endH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MeditechCyanDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Titolo Appuntamento
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MeditechTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Dettagli Sede & Data
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MeditechCyanPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Data: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeditechTextSecondary
                )
            }

            if (event.location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MeditechCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MeditechTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Link Google Meet Videochiamata (se presente)
            if (event.isOnline && event.hangoutLink.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MeditechCyanSoft.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, MeditechCyanPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val meetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.hangoutLink))
                            meetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(meetIntent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Partecipa al Meeting Clinico Online",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MeditechCyanDark,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MeditechCyanDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Riga Pulsanti Azione Tattili
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Apri Mappe
                val mapsInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val loc = event.location.ifBlank { "SUPSI Viganello Lugano" }
                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(loc)))
                        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(mapIntent)
                    },
                    interactionSource = mapsInteraction,
                    shape = RoundedCornerShape(10.dp),
                    color = MeditechGroupedBg,
                    border = BorderStroke(0.6.dp, MeditechBorder),
                    modifier = Modifier
                        .weight(1f)
                        .cartoonPressSquash(mapsInteraction, pressedScaleY = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MeditechCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Mappe",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MeditechTextPrimary
                        )
                    }
                }

                // 2. Condividi Impegno
                val shareInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val shareText = buildString {
                            append("🩺 Appuntamento MeDiTech SUPSI:\n")
                            append("📌 ${event.title}\n")
                            append("🗓 Data: $dateStr ($startH - $endH)\n")
                            if (event.location.isNotBlank()) append("📍 Sede: ${event.location}\n")
                            if (event.isOnline && event.hangoutLink.isNotBlank()) {
                                append("💻 Videochiamata: ${event.hangoutLink}\n")
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val chooser = Intent.createChooser(sendIntent, "Condividi Appuntamento Clinico:")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    },
                    interactionSource = shareInteraction,
                    shape = RoundedCornerShape(10.dp),
                    color = MeditechCyanSoft,
                    border = BorderStroke(0.6.dp, MeditechCyanPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .cartoonPressSquash(shareInteraction, pressedScaleY = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MeditechCyanDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Condividi",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MeditechCyanDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pulsante compatto tattile inline sotto la bolla (in stile Assistente Perito).
 */
@Composable
fun MeditechStudioInlineButton(
    text: String,
    leadingIcon: ImageVector? = null,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = 0.5.dp,
        border = borderColor?.let { BorderStroke(0.6.dp, it) },
        modifier = modifier
            .heightIn(min = 42.dp)
            .cartoonPressSquash(interactionSource, pressedScaleY = 0.93f, squashScaleX = 1.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = (-0.2).sp
                ),
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
