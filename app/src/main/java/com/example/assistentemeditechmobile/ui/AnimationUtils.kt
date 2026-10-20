package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Emil Kowalski Bouncy Micro-Interaction (/emilkowalski-animation).
 * Runs strictly on GPU RenderThread via graphicsLayer (skips Recomposition & Layout).
 * Delivers satisfying, cartoony tactile feedback on button/card presses.
 */
fun Modifier.emilPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f
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
 * High-bounce cartoony spring spec for entrance pops and tab gliding.
 */
val CartoonyBouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

val CartoonyFastBounce = spring<Float>(
    dampingRatio = Spring.DampingRatioHighBouncy,
    stiffness = Spring.StiffnessMedium
)
