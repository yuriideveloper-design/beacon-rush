package com.pixelhaven.beaconrush.quay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val NightInk = Color(0xFF1C1C1C)
private val NightLift = Color(0xFF2A241C)
private val AmberCore = Color(0xFFE8A14A)
private val AmberSoft = Color(0xCCE8A14A)
private val AmberWash = Color(0x33E8A14A)
private val FogText = Color(0xFFEDE6D6)

@Composable
fun QuaySplash(
    readyToLeave: Boolean = false,
    onFinished: (() -> Unit)? = null,
) {
    val sweep = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    var motionDone by remember { mutableStateOf(false) }
    val glow = rememberInfiniteTransition(label = "beaconGlow")
    val halo by glow.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo",
    )

    fun finish() {
        if (onFinished == null || finished) return
        finished = true
        onFinished()
    }

    LaunchedEffect(onFinished) {
        if (onFinished == null) {
            while (true) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(2800, easing = LinearEasing))
            }
        } else {
            sweep.animateTo(1f, tween(2800, easing = LinearEasing))
            delay(700)
            motionDone = true
        }
    }
    LaunchedEffect(motionDone, readyToLeave) {
        if (motionDone && readyToLeave) finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NightLift, NightInk, Color(0xFF101010)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(196.dp)) {
                val cx = size.width / 2f
                val ground = size.height * 0.86f
                val towerTop = size.height * 0.34f
                val lanternY = size.height * 0.26f
                val beamAngle = Math.toRadians((-38.0 + 76.0 * sweep.value.toDouble()))
                val beamLen = size.minDimension * 0.62f
                val tip = Offset(
                    cx + beamLen * cos(beamAngle).toFloat(),
                    lanternY + beamLen * 0.18f * sin(beamAngle).toFloat(),
                )
                val spread = 26f + 18f * halo
                val beam = Path().apply {
                    moveTo(cx, lanternY)
                    lineTo(tip.x - spread, tip.y + 18f)
                    lineTo(tip.x + spread, tip.y + 18f)
                    close()
                }
                drawPath(beam, AmberWash.copy(alpha = 0.18f + 0.22f * halo))
                drawCircle(
                    color = AmberCore.copy(alpha = halo),
                    radius = 18f + 10f * halo,
                    center = Offset(cx, lanternY),
                )
                drawCircle(
                    color = AmberSoft,
                    radius = 7f,
                    center = Offset(cx, lanternY),
                )
                val tower = Path().apply {
                    moveTo(cx - 16f, towerTop)
                    lineTo(cx + 16f, towerTop)
                    lineTo(cx + 28f, ground)
                    lineTo(cx - 28f, ground)
                    close()
                }
                drawPath(tower, Color(0xFF3A3328))
                drawLine(
                    AmberCore.copy(alpha = 0.35f),
                    Offset(cx, towerTop + 8f),
                    Offset(cx, ground - 6f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = AmberCore.copy(alpha = 0.2f),
                    radius = 34f,
                    center = Offset(cx, lanternY),
                    style = Stroke(width = 2.2f),
                )
                drawLine(
                    Color(0xFF4A4338),
                    Offset(cx - 40f, ground),
                    Offset(cx + 40f, ground),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "BuІІ Rush",
                color = FogText,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Lighting the night lane",
                color = AmberCore,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
