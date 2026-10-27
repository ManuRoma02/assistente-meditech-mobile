package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistentemeditechmobile.theme.*
import kotlinx.coroutines.delay

/**
 * Emil Kowalski & Apple HIG Animation Architecture (/emilkowalski-animation).
 * Runs strictly on GPU RenderThread via graphicsLayer (skips Recomposition & Layout).
 * Delivers clean, tactile, physics-based motion with haptic synchronization.
 */

val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Standard Clinical Motion Tokens:
 * Decoupled from rubber-banding for high-frequency clinical interactions (100+/day).
 */
val MeditechClinicalSpring = spring<Float>(
    dampingRatio = 0.78f, // Assestamento rapido senza oscillazioni multiple fastidiose
    stiffness = 450f
)

val MeditechTactilePressSpring = spring<Float>(
    dampingRatio = 0.72f, // Risposta immediata al pollice con rimbalzo sottile
    stiffness = 600f
)

val MeditechSheetSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 380f
)

val MeditechDecelEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

val CartoonyBouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

val CartoonyHighBouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioHighBouncy,
    stiffness = Spring.StiffnessMedium
)

val CartoonySnappySpring = spring<Float>(
    dampingRatio = 0.58f,
    stiffness = 450f
)

/**
 * Emil Kowalski Press Feedback on GPU RenderThread with tactile Haptics.
 * Calibrated scale (0.975f for cards, 0.94f for small buttons) + immediate haptic tick.
 */
fun Modifier.emilPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.975f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            runCatching {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MeditechTactilePressSpring,
        label = "emilPressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Cartoon Poof & Disintegration Animation for Clearing Chat History.
 * Staggered per-message delay: each bubble squashes, tilts playfully,
 * drifts upwards like a cloud/bubble, and fades away!
 */
fun Modifier.cartoonPoofDissolve(
    isDissolving: Boolean,
    index: Int = 0
): Modifier = composed {
    val staggerDelay = (index * 35).coerceAtMost(200)

    val scale by animateFloatAsState(
        targetValue = if (isDissolving) 0.68f else 1f,
        animationSpec = if (isDissolving) {
            tween(durationMillis = 350, delayMillis = staggerDelay, easing = FastOutLinearInEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "poof_scale"
    )

    val translationY by animateFloatAsState(
        targetValue = if (isDissolving) -90f else 0f,
        animationSpec = if (isDissolving) {
            tween(durationMillis = 380, delayMillis = staggerDelay, easing = FastOutLinearInEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "poof_translationY"
    )

    val rotationZ by animateFloatAsState(
        targetValue = if (isDissolving) {
            if (index % 2 == 0) -8f else 8f
        } else 0f,
        animationSpec = if (isDissolving) {
            tween(durationMillis = 350, delayMillis = staggerDelay)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "poof_rotation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDissolving) 0f else 1f,
        animationSpec = if (isDissolving) {
            tween(durationMillis = 300, delayMillis = staggerDelay + 60)
        } else {
            tween(durationMillis = 200)
        },
        label = "poof_alpha"
    )

    this.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
        this.translationY = translationY
        this.rotationZ = rotationZ
        this.alpha = alpha
    }
}

/**
 * Cartoon Paper Plane Flight for Email Sending.
 * Flies up and right with rotation and fade, delivering instant delight.
 */
fun Modifier.cartoonPaperPlane(
    isLaunching: Boolean
): Modifier = composed {
    val translationX by animateFloatAsState(
        targetValue = if (isLaunching) 320f else 0f,
        animationSpec = if (isLaunching) tween(380, easing = FastOutLinearInEasing) else spring(),
        label = "plane_tx"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isLaunching) -140f else 0f,
        animationSpec = if (isLaunching) tween(380, easing = FastOutLinearInEasing) else spring(),
        label = "plane_ty"
    )
    val rotationZ by animateFloatAsState(
        targetValue = if (isLaunching) -35f else 0f,
        animationSpec = if (isLaunching) tween(380) else spring(),
        label = "plane_rot"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isLaunching) 0f else 1f,
        animationSpec = if (isLaunching) tween(300, delayMillis = 80) else tween(150),
        label = "plane_alpha"
    )

    this.graphicsLayer {
        this.translationX = translationX
        this.translationY = translationY
        this.rotationZ = rotationZ
        this.alpha = alpha
    }
}

/**
 * Cartoon Card Dismissal for Calendar Appointment cancellation.
 * Slides horizontally to the right with squash and fade.
 */
fun Modifier.cartoonCardExit(
    isExiting: Boolean
): Modifier = composed {
    val translationX by animateFloatAsState(
        targetValue = if (isExiting) 450f else 0f,
        animationSpec = if (isExiting) tween(320, easing = FastOutLinearInEasing) else spring(),
        label = "card_exit_tx"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isExiting) 0.5f else 1f,
        animationSpec = if (isExiting) tween(320) else spring(),
        label = "card_exit_sy"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = if (isExiting) tween(280) else tween(150),
        label = "card_exit_alpha"
    )

    this.graphicsLayer {
        this.translationX = translationX
        this.scaleY = scaleY
        this.alpha = alpha
    }
}

/**
 * Pulsing acoustic breathing animation for voice microphone.
 */
fun Modifier.cartoonAcousticPulse(
    isListening: Boolean
): Modifier = composed {
    if (!isListening) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    this.graphicsLayer {
        scaleX = pulseScale
        scaleY = pulseScale
    }
}

/**
 * Organic Cartoon Press Squash Feedback on GPU RenderThread.
 * Compresses Y and bulges X slightly on press, then springs back like physical rubber.
 */
fun Modifier.cartoonPressSquash(
    interactionSource: InteractionSource,
    pressedScaleY: Float = 0.92f,
    squashScaleX: Float = 1.05f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) pressedScaleY else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cartoon_squash_y"
    )
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) squashScaleX else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cartoon_squash_x"
    )
    this.graphicsLayer {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }
}

/**
 * Emil Kowalski Spring Entrance for Cards, Messages and Dialogs.
 * Enters smoothly from 0.94f to 1.0f (NEVER from 0f!) with subtle vertical slide and alpha.
 */
fun Modifier.springEntrance(
    delayMillis: Int = 0
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        isVisible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "entrance_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = LinearEasing),
        label = "entrance_alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 18f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "entrance_ty"
    )
    this.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
        this.alpha = alpha
        this.translationY = translationY
    }
}

/**
 * SettingsTendinaCard: Sezione espandibile a tendina in stile Apple HIG & Perito Mobile.
 * - Chevron che ruota a 180° su GPU RenderThread con molla Emil Kowalski
 * - animateContentSize con molla fisica naturale (dampingRatio = 0.82f)
 * - Badge opzionale con colore MeDiTech
 * - Bordo e contrasto dinamico all'apertura
 */
@Composable
fun SettingsTendinaCard(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = MeditechCyanSoft,
    badgeTextColor: Color = MeditechCyanDark,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tendina_chevron"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MeditechGroupedBg,
        border = BorderStroke(
            if (isExpanded) 1.dp else 0.6.dp,
            if (isExpanded) MeditechCyanPrimary.copy(alpha = 0.65f) else MeditechBorder
        ),
        shadowElevation = if (isExpanded) 1.5.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring<IntSize>(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione a Tendina Cliccabile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MeditechTextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeditechTextTertiary,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Freccia Chevron animata con rotazione fluida su GPU
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (isExpanded) MeditechCyanSoft else MeditechSurface,
                            CircleShape
                        )
                        .border(
                            0.6.dp,
                            if (isExpanded) MeditechBorderCyan else MeditechBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi sezione" else "Espandi sezione",
                        tint = if (isExpanded) MeditechCyanDark else MeditechTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            // Corpo Dettagliato a Tendina
            if (isExpanded) {
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MeditechBorder,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}
