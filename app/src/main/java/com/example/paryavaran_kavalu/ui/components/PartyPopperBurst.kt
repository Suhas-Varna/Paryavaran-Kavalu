package com.example.paryavaran_kavalu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private val ConfettiPalette = listOf(
    Color(0xFFE91E63),
    Color(0xFFFFC107),
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFF5722),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFFFFEB3B),
    Color(0xFFE040FB),
    Color(0xFF76FF03),
)

private enum class ConfettiKind { Dot, Pill, Square }

private data class ConfettiPiece(
    val angleRad: Float,
    val maxDistance: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val color: Color,
    val kind: ConfettiKind,
    val phase: Float,
    val spinDir: Float,
)

private fun buildConfettiPieces(seed: Long): List<ConfettiPiece> {
    val r = Random(seed)
    return List(96) {
        val angle = r.nextFloat() * 2f * PI.toFloat()
        val kind = when (r.nextInt(10)) {
            in 0..4 -> ConfettiKind.Dot
            in 5..7 -> ConfettiKind.Pill
            else -> ConfettiKind.Square
        }
        val (hw, hh) = when (kind) {
            ConfettiKind.Dot -> {
                val s = 3f + r.nextFloat() * 5f
                s to s
            }
            ConfettiKind.Pill -> {
                val w = 3f + r.nextFloat() * 4f
                val h = 8f + r.nextFloat() * 14f
                w to h
            }
            ConfettiKind.Square -> {
                val s = 5f + r.nextFloat() * 7f
                s to s
            }
        }
        ConfettiPiece(
            angleRad = angle,
            maxDistance = 90f + r.nextFloat() * 340f,
            halfWidth = hw,
            halfHeight = hh,
            color = ConfettiPalette[r.nextInt(ConfettiPalette.size)].copy(alpha = 0.92f),
            kind = kind,
            phase = r.nextFloat() * 0.14f,
            spinDir = if (r.nextBoolean()) 1f else -1f,
        )
    }
}

/**
 * Festive burst + falling confetti — pairs with success moments (cleanup verified, reward redeemed).
 */
@Composable
fun PartyPopperBurst(
    modifier: Modifier = Modifier,
    triggerKey: Any,
    originYFraction: Float = 0.26f,
) {
    val burst = remember { Animatable(0f) }
    val pieces = remember { buildConfettiPieces(Random.nextLong()) }

    LaunchedEffect(triggerKey) {
        burst.snapTo(0f)
        delay(420)
        burst.animateTo(
            1f,
            animationSpec = tween(1750, easing = FastOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * originYFraction
        val p = burst.value
        val gravity = 420f

        pieces.forEach { piece ->
            val start = piece.phase
            val denom = (1f - start).coerceAtLeast(0.05f)
            val localT = if (p <= start) {
                0f
            } else {
                ((p - start) / denom).coerceIn(0f, 1f)
            }
            if (localT <= 0.001f) return@forEach
            val eased = FastOutSlowInEasing.transform(localT)
            val dist = piece.maxDistance * eased
            val x = cx + cos(piece.angleRad) * dist
            val y = cy + sin(piece.angleRad) * dist + gravity * localT * localT
            val fade = when {
                localT < 0.08f -> localT / 0.08f
                localT > 0.78f -> 1f - (localT - 0.78f) / 0.22f
                else -> 1f
            }.coerceIn(0f, 1f)
            val c = piece.color.copy(alpha = piece.color.alpha * fade)

            rotate(
                degrees = piece.spinDir * localT * 520f,
                pivot = Offset(x, y),
            ) {
                when (piece.kind) {
                    ConfettiKind.Dot -> drawCircle(
                        color = c,
                        radius = piece.halfWidth,
                        center = Offset(x, y),
                    )
                    ConfettiKind.Pill -> drawRoundRect(
                        color = c,
                        topLeft = Offset(x - piece.halfWidth, y - piece.halfHeight),
                        size = Size(piece.halfWidth * 2f, piece.halfHeight * 2f),
                        cornerRadius = CornerRadius(piece.halfWidth, piece.halfWidth),
                    )
                    ConfettiKind.Square -> drawRoundRect(
                        color = c,
                        topLeft = Offset(x - piece.halfWidth, y - piece.halfHeight),
                        size = Size(piece.halfWidth * 2f, piece.halfHeight * 2f),
                        cornerRadius = CornerRadius(2.5f, 2.5f),
                    )
                }
            }
        }
    }
}
