package com.example.assistentemeditechmobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistentemeditechmobile.theme.MeditechCyanDark
import com.example.assistentemeditechmobile.theme.MeditechCyanPrimary
import com.example.assistentemeditechmobile.theme.MeditechCyanSoft
import com.example.assistentemeditechmobile.theme.MeditechTealActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

// 3D Medical Cross / Clinical Node Vertices
private val ClinicalCrossVertices = listOf(
    Vec3(-0.45f, 0f, 0f), Vec3(0.45f, 0f, 0f), // Horizontal bar
    Vec3(0f, -0.45f, 0f), Vec3(0f, 0.45f, 0f), // Vertical bar
    Vec3(0f, 0f, -0.45f), Vec3(0f, 0f, 0.45f)  // Depth axis
)

// 3D Fibonacci Neural Sphere Nodes
private val FibonacciSpherePoints: List<Vec3> = buildList {
    val count = 22
    val goldenAngle = PI.toFloat() * (3f - sqrt(5f))
    for (i in 0 until count) {
        val y = 1f - (i.toFloat() / (count - 1).toFloat()) * 2f
        val radius = sqrt((1f - y * y).coerceAtLeast(0f))
        val theta = goldenAngle * i
        add(Vec3(cos(theta) * radius, y, sin(theta) * radius))
    }
}

/**
 * Interactive 3D MeDiTech Neural Orb (/threejs & /apple-design).
 * - Real-time 3D projection with volumetric cyan/teal bloom
 * - Fibonacci sphere nodes with depth sorting
 * - Interactive touch OrbitControls (drag to rotate 3D sphere)
 * - Rhythmic pulsing when AI is processing
 */
@Composable
fun Meditech3DNeuralOrb(
    isProcessing: Boolean,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "meditech_orb_rotation")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 1400 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )

    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isProcessing) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 400 else 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    var userYaw by remember { mutableFloatStateOf(0.4f) }
    var userPitch by remember { mutableFloatStateOf(-0.25f) }

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Cartoony spin acceleration on tap
                        userYaw += 3.14159f
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    userYaw += dragAmount.x * 0.02f
                    userPitch = (userPitch + dragAmount.y * 0.02f).coerceIn(-1.2f, 1.2f)
                }
            }
            .semantics {
                contentDescription = if (isProcessing) "MeDiTech AI in elaborazione" else "MeDiTech AI pronto"
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = (this.size.minDimension / 2f) * 0.85f * animatedPulse

        // 1. Volumetric Clinical Cyan Ambient Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    MeditechCyanPrimary.copy(alpha = if (isProcessing) 0.55f else 0.30f),
                    MeditechTealActive.copy(alpha = if (isProcessing) 0.28f else 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.35f
            ),
            radius = r * 1.35f,
            center = Offset(cx, cy)
        )

        // 2. High-Tech 3D Sphere Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE0F2FE),
                    MeditechCyanPrimary,
                    MeditechCyanDark,
                    Color(0xFF034A6E)
                ),
                center = Offset(cx - r * 0.28f, cy - r * 0.28f),
                radius = r * 1.15f
            ),
            radius = r * 0.86f,
            center = Offset(cx, cy)
        )

        // 3. 3D Rotation Transform Matrix
        val totalYaw = animatedPhase + userYaw
        val cosY = cos(totalYaw)
        val sinY = sin(totalYaw)
        val cosP = cos(userPitch)
        val sinP = sin(userPitch)

        fun project(v: Vec3): Triple<Float, Float, Float> {
            val x1 = v.x * cosY - v.z * sinY
            val z1 = v.x * sinY + v.z * cosY
            val y2 = v.y * cosP - z1 * sinP
            val z2 = v.y * sinP + z1 * cosP
            val fov = 2.4f / (2.4f - z2 * 0.5f)
            val px = cx + x1 * r * fov * 0.72f
            val py = cy + y2 * r * fov * 0.72f
            return Triple(px, py, z2)
        }

        // Draw 3D Cross Axis
        for (i in 0 until ClinicalCrossVertices.size step 2) {
            val p1 = project(ClinicalCrossVertices[i])
            val p2 = project(ClinicalCrossVertices[i + 1])
            val avgZ = (p1.third + p2.third) / 2f
            val alpha = ((avgZ + 1f) / 2f).coerceIn(0.25f, 0.95f)
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(p1.first, p1.second),
                end = Offset(p2.first, p2.second),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw Fibonacci Orbit Points
        for (node in FibonacciSpherePoints) {
            val proj = project(node)
            val z = proj.third
            val alpha = ((z + 1.1f) / 2.2f).coerceIn(0.2f, 1f)
            val pointRadius = (2.2.dp.toPx() * (1f + z * 0.35f)).coerceAtLeast(1.2.dp.toPx())
            drawCircle(
                color = if (z > 0) Color.White.copy(alpha = alpha) else MeditechCyanSoft.copy(alpha = alpha * 0.6f),
                radius = pointRadius,
                center = Offset(proj.first, proj.second)
            )
        }
    }
}
