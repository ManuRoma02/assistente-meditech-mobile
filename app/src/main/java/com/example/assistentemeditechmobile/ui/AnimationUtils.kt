package com.example.assistentemeditechmobile.ui

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
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Emil Kowalski & Cartoony Animation Architecture (/emilkowalski-animation).
 * Runs strictly on GPU RenderThread via graphicsLayer (skips Recomposition & Layout).
 * Delivers juicy, tactile, physics-based cartoon motion for all user actions.
 */

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
 * Emil Kowalski Press Feedback on GPU RenderThread.
 * Scales down on press, springs back with satisfying tactile bounce.
 */
fun Modifier.emilPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.93f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
